#!/usr/bin/env python3
"""解析 atrace 文本 trace，定位主线程卡顿的 culprit。

用法: jank-parse.py <trace-file> [app-pid]
      app-pid 省略时自动识别（先找 `alist:` 标记，再找 VRI[MainActivity]）。

支持四种 marker 形态:
    B|pid|name / E|pid            同步段（同线程栈式配对）
    S|pid|name|cookie / F|...     异步段（跨线程，按 cookie 配对）

输出:
  1. 冷启动里程碑（alist: 异步段 + 关键同步段）按时间排序
  2. 主线程 self-time 排行 —— 直接回答"谁吃掉了主线程时间"
  3. >32ms 的 doFrame 间隔，以及窗口内的 self-time 排行
"""
import re
import sys
from collections import defaultdict

LINE_RE = re.compile(
    r'^\s*(?P<comm>.+?)-(?P<tid>\d+)\s+\(\s*(?P<tgid>\d+)\)\s+\[(?P<cpu>\d+)\]\s+\S+\s+'
    r'(?P<ts>\d+\.\d+):\s+(?P<event>\S+?):\s*(?P<rest>.*)$'
)
MARK_RE = re.compile(r'^([BEFSC])\|(\d+)\|?([^|]*)\|?([^|]*)$')

ALIST = 'alist:'
JANK_MS = 32.0


class Node:
    __slots__ = ('name', 'start', 'end', 'children', 'pid')

    def __init__(self, name, start, pid):
        self.name, self.start, self.end, self.pid = name, start, None, pid
        self.children = []

    @property
    def dur_ms(self):
        return 0.0 if self.end is None else (self.end - self.start) * 1000.0

    def self_ms(self):
        return self.dur_ms - sum(c.dur_ms for c in self.children)


class Trace:
    def __init__(self, path):
        self.roots = defaultdict(list)      # tid -> [Node]
        self.asyncs = []                    # Node (pid-keyed)
        self.raw = []                       # (pid, ts, kind, name)
        self._read(path)

    def _read(self, path):
        stacks = defaultdict(list)
        open_async = {}
        with open(path, 'r', errors='replace') as fh:
            for line in fh:
                m = LINE_RE.match(line)
                if not m or m.group('event') != 'tracing_mark_write':
                    continue
                mm = MARK_RE.match(m.group('rest').strip())
                if not mm:
                    continue
                kind, pid, name, cookie = mm.group(1), int(mm.group(2)), mm.group(3), mm.group(4)
                tid, ts = int(m.group('tid')), float(m.group('ts'))
                self.raw.append((pid, ts, kind, name))
                if kind == 'B':
                    node = Node(name, ts, pid)
                    if stacks[tid]:
                        stacks[tid][-1].children.append(node)
                    else:
                        self.roots[tid].append(node)
                    stacks[tid].append(node)
                elif kind == 'E':
                    if stacks[tid]:
                        stacks[tid].pop().end = ts
                elif kind == 'S' and cookie:
                    open_async[(pid, cookie)] = Node(name, ts, pid)
                elif kind == 'F' and cookie:
                    node = open_async.pop((pid, cookie), None)
                    if node is not None:
                        node.end = ts
                        self.asyncs.append(node)

    # -- helpers ---------------------------------------------------------
    def all_nodes(self, tid=None):
        """深度优先遍历；tid 为 None 时遍历所有线程。"""
        src = self.roots[tid] if tid is not None else [n for lst in self.roots.values() for n in lst]
        stack = list(reversed(src))
        while stack:
            n = stack.pop()
            yield n
            stack.extend(reversed(n.children))

    def in_window(self, tid, start, end):
        return [n for n in self.all_nodes(tid) if n.start >= start and (n.end or n.start) <= end]


def rank_self(nodes, top=25, floor_ms=1.0):
    agg = defaultdict(float)
    cnt = defaultdict(int)
    for n in nodes:
        s = n.self_ms()
        if s >= floor_ms:
            agg[n.name] += s
            cnt[n.name] += 1
    return sorted(((v, k, cnt[k]) for k, v in agg.items()), reverse=True)[:top]


def detect_pid(trace):
    pids = {pid for pid, _, kind, name in trace.raw if kind == 'B' and name.startswith(ALIST)}
    if pids:
        return sorted(pids)[0]
    pids = {pid for pid, _, kind, name in trace.raw if kind == 'B' and 'VRI[MainActivity]' in name}
    return sorted(pids)[0] if pids else None


def main():
    path = sys.argv[1]
    trace = Trace(path)
    pid = int(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2].strip() else detect_pid(trace)

    print(f'== trace {path}')
    print(f'== app_pid={pid}  同步段={sum(len(v) for v in trace.roots.values())} 根  异步段={len(trace.asyncs)}')
    if pid is None:
        print('!! 无法识别 app pid，且 -a <pkg> 未生效（没有任何 alist: 标记）')
        return

    # 1. 冷启动里程碑
    print('\n== 冷启动里程碑（相对第一个 marker 的偏移）==')
    interesting = [n for n in trace.asyncs if n.pid == pid and n.name.startswith(ALIST)]
    interesting += [n for n in trace.all_nodes(pid)
                    if n.name in ('Choreographer#doFrame',) and n.dur_ms > JANK_MS]
    if interesting:
        t0 = min(n.start for n in interesting)
        for n in sorted(interesting, key=lambda n: n.start)[:60]:
            print(f'  +{(n.start - t0) * 1000:8.1f}ms  {n.dur_ms:8.2f}ms  {n.name}')
    else:
        print('  (没有任何 alist: 或慢帧标记)')

    # 2. 主线程 self-time 排行
    main_nodes = list(trace.all_nodes(pid))
    print(f'\n== 主线程 self-time 排行（>1ms，共 {len(main_nodes)} 个段）==')
    print('   self_ms   次数  section')
    for total, name, count in rank_self(main_nodes):
        print(f'  {total:8.1f}  {count:5d}  {name}')

    # 3. 慢帧窗口
    frames = sorted(n.start for n in main_nodes if n.name.startswith('Choreographer#doFrame'))
    if len(frames) > 1:
        gaps = [(frames[i + 1], (frames[i + 1] - frames[i]) * 1000.0) for i in range(len(frames) - 1)]
        ds = sorted(g for _, g in gaps)
        print(f'\n== doFrame 间隔 n={len(gaps)} p50={ds[len(ds) // 2]:.1f}ms '
              f'p90={ds[int(len(ds) * .9)]:.1f}ms max={ds[-1]:.1f}ms jank(>32ms)={sum(1 for x in ds if x > JANK_MS)}')
        for start, gap in sorted(gaps, key=lambda x: -x[1])[:5]:
            end = start + gap / 1000.0
            print(f'\n-- 慢帧窗口 {gap:.1f}ms  t={start:.3f} → {end:.3f}')
            inner = trace.in_window(pid, start, end)
            if not inner:
                print('   主线程窗口内 0 个段 → 阻塞在未插桩/非 app 路径')
            for total, name, count in rank_self(inner, top=15, floor_ms=2.0):
                print(f'   self {total:8.1f}ms  x{count:<4d} {name}')
            for n in sorted(trace.asyncs, key=lambda n: n.start):
                if n.pid == pid and n.start <= end and (n.end or n.start) >= start:
                    print(f'   async {n.dur_ms:8.2f}ms  {n.name}')


if __name__ == '__main__':
    main()
