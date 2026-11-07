# 纽曼GPS (NewmanGPS)

<p align="center">
  <img src="https://img.shields.io/badge/Android-SDK%2036-3DDC84?style=flat-square&logo=android&logoColor=white" alt="SDK 36" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10.01-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose BOM" />
  <img src="https://img.shields.io/badge/UI-Miuix%20Compose-FF6900?style=flat-square" alt="Miuix Compose" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License MIT" />
</p>

纽曼（Newsmy）GPS 定位器第三方 Android 客户端，基于 **Jetpack Compose** 与开源 **Miuix UI** 设计组件开发，面向 Android 16 (SDK 36)。

---

## 功能特性

- **设备状态与遥测**：查看定位器实时经纬度、卫星更新时间、运行速度、静止/休眠状态，按位解析电池实际剩余百分比。
- **实时定位跟踪**：展示目标位置火星坐标（GCJ-02），提供高精度 WGS-84 与 GCJ-02 双向坐标转换。
- **历史轨迹回放**：支持查询最近 48 小时历史点位，提供倍速播放与进度调节。
- **电子围栏管理**：查询已配置的围栏范围与报警状态。
- **报警与后台轮询**：支持官方个推通道以及基于 WorkManager 的后台定时轮询兜底，及时推送位移、出入围栏与低电量告警。

---

## 技术架构

- **开发语言**：Kotlin 2.0.21
- **UI 框架**：Jetpack Compose + [top.yukonga.miuix.kmp](https://github.com/compose-miuix-ui/miuix)
- **网络通信**：OkHttp 4 + 自定义通讯散列验签与 Base64 封包
- **后台任务**：AndroidX WorkManager
- **最低支持**：Android 8.0 (API 26)
- **目标 SDK**：Android 16 (API 36)

---

## 编译构建

### 1. 环境要求
- JDK 17 或 21
- Android SDK (API 36, Build-Tools 36.0.0+)
- Gradle 9.7.0 (已包含 gradlew 脚本)

### 2. 本地构建
```bash
# Debug 版本
./gradlew assembleDebug

# Release 签名版本
./gradlew assembleCiRelease
```
产物输出路径为 `app/build/outputs/apk/`。

---

## GitHub Actions 自动打包

项目已配置自动编译工作流（`.github/workflows/android-release.yml`）。

### 1. 配置签名密钥
在 GitHub 仓库 **Settings -> Secrets and variables -> Actions** 中添加以下环境变量：

- `SIGNING_STORE_BASE64`：签名证书文件（.jks/.keystore）的 Base64 编码字符串
- `ANDROID_KEYSTORE_PASSWORD`：密钥库密码
- `ANDROID_KEY_ALIAS`：密钥别名
- `ANDROID_KEY_PASSWORD`：密钥私钥密码

### 2. 发布发版
推送 `v*` 格式的版本标签即可自动触发打包并创建 GitHub Release：
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## 免责声明

1. 本项目为个人开发的开源第三方客户端，仅供学习、交流与技术研究使用。
2. 本项目与“纽曼”（Newsmy）官方无商业关联。
