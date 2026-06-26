# Alist Android Client

Android 原生 Alist v3 文件管理客户端 MVP。

## 功能

- Alist v3 账号密码登录
- 文件浏览、搜索、排序
- 上传、下载、删除、新建文件夹、重命名、复制、移动
- 上传/下载任务列表
- 图片/文本基础预览
- 系统分享和链接分享
- 单账号设置与退出登录

## 构建

```bash
./gradlew :app:assembleDebug
```

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 测试

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

连接设备后：

```bash
./gradlew :app:connectedDebugAndroidTest
```

## 已知限制

见 `docs/testing/known-limitations.md`。