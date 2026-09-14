#!/usr/bin/env bash
# 项目开发环境 - 用时 source 一下,不污染 shell 全局
# 用法: source tools/dev-env.sh
#
# 切到 Java 17(项目 JVM target 17),并把 Android SDK 路径加到 PATH
# 注意:这里用绝对路径硬编码,如果环境换了需要改

# === Java 17(项目要求)===
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
# 把 JAVA_HOME/bin 提前到 PATH 最前,确保 java/javac 走 17
# (系统 PATH 里 ~/.jdks/corretto-1.8.0_504/bin 通常在最前,会先抢到)
export PATH="$JAVA_HOME/bin:$PATH"

# === Android SDK(写到 ~/.bashrc 后已经全局可见,这里再次确保)===
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"

# === 代理(TUN 模式不接管 JVM 进程 DNS,gradle wrapper / sdkmanager 走 mihomo 7897)===
# mihomo mixed-port 127.0.0.1:7897,支持 HTTP + SOCKS5
# JAVA_TOOL_OPTIONS 是 JVM 启动时自动读取的,gradle daemon / sdkmanager / GradleWrapperMain 都生效
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 -DsocksProxyHost=127.0.0.1 -DsocksProxyPort=7897"

# 提示
echo "[dev-env] Java:    $(java -version 2>&1 | head -1)"
echo "[dev-env] ANDROID_HOME=$ANDROID_HOME"
echo "[dev-env] Proxy:   http/https/socks 127.0.0.1:7897 (mihomo TUN 不接管 JVM DNS 时用)"
