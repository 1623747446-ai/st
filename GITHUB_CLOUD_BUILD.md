# 无本地 Android 环境：GitHub Actions 云端打包

这个仓库已经配置好 `.github/workflows/build-apk.yml`。

你不需要安装 Android Studio、Gradle 或 Android SDK。

## 操作

1. GitHub 新建一个空仓库。
2. 把这个项目的全部文件上传到仓库根目录。
3. 打开仓库顶部的 **Actions**。
4. 点击左侧 **Build Android APK**。
5. 点击 **Run workflow**。
6. 等待构建完成。
7. 打开完成的构建记录。
8. 在页面底部 **Artifacts** 下载：

   `ProtocolSignatureLabV1-APK`

解压后得到：

`ProtocolSignatureLabV1.apk`

## 自动构建

每次 push 到 `main` 或 `master` 也会自动重新打包。
