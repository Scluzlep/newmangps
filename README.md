# 纽曼GPS (NewmanGPS) · HyperOS Native Edition

<p align="center">
  <img src="https://img.shields.io/badge/Android-SDK%2036-3DDC84?style=flat-square&logo=android&logoColor=white" alt="SDK 36" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10.01-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose BOM" />
  <img src="https://img.shields.io/badge/Design-Xiaomi%20HyperOS%20%2F%20MIUIX-FF6900?style=flat-square" alt="HyperOS Design" />
  <img src="https://img.shields.io/badge/License-MIT-blue.style=flat-square" alt="License MIT" />
</p>

一款专为纽曼（Newsmy）系列 GPS 车载/个人定位器打造的 **现代化、极致轻量、原生体验** 的 Android 客户端。

采用 **Jetpack Compose + Xiaomi HyperOS / MIUIX 设计语言**，针对 Android 16 (SDK 36) 进行了深度前沿适配，告别官方客户端基于 Webview 封装所带来的卡顿、臃肿、发热与多余商业弹窗。

---

## 🌟 核心特性与优势对比

| 维度 | 官方客户端 (Newsmy Android) | 本项目 (NewmanGPS Native) |
| :--- | :--- | :--- |
| **底层架构** | DCloud uni-app WebView 套壳混合应用 | 100% 现代 Kotlin + Jetpack Compose 原生应用 |
| **内存与功耗** | 运行时占用 200MB+ 内存，常驻 WebView 渲染进程 | 原生渲染，运存占用通常低于 40MB，低电耗 |
| **启动与响应** | 启动需加载本地离线包及远程 Web 资源，耗时约 3~5 秒 | 冷启动 **< 300 毫秒**，毫秒级即时响应 |
| **UI / 动效** | 传统老旧 HTML5 样式，滑动手感生硬 | 遵循 **小米澎湃 OS (Xiaomi HyperOS / MIUIX)** 设计语言，连续曲率圆角与柔和微交互 |
| **硬件遥测解析** | 前端隐藏/折叠真实状态，部分状态受限展示 | 完整逆向底层通信协议，精准按位解析设备真实电量（8位掩码提取）、实时速度、静止/休眠状态 |
| **报警与推送** | 强依赖第三方平台推送服务，易被后台冻结 | **双重推送机制**：支持官方个推通道 + WorkManager 自主后台静默轮询 Fallback |
| **权限与隐私** | 包含广告追踪、支付集成等多余 SDK | **零多余 SDK**，不收集任何用户数据，密钥仅保存在设备本地 |

---

## 🎨 HyperOS / MIUIX 设计规范

本项目深度还原了小米澎湃 OS (Xiaomi HyperOS) 与 MIUIX 的核心视觉与触觉质感：

- **连续曲率平滑圆角（Squircles）**：全界面采用 `22.dp` 与 `16.dp` 连续平滑倒角容器，柔和自然。
- **Hyper 色彩系统**：以 Hyper 极光蓝 (`#007DFF`) 为主品牌色，搭配翡翠绿 (`#34C759`) 在线胶囊与珊瑚红 (`#FF3B30`) 状态指示。
- **层叠卡片布局**：高对比度的内容模块分层，关键遥测数据（电量、坐标、航速、工作模式）一目了然。
- **沉浸式边缘到边缘（Edge-to-Edge）**：完美适配全面屏手势、系统状态栏与导航栏变色。

---

## 🛠️ 底层协议逆向与技术解析

客户端直接与纽曼定位器云端 RPC 集群进行高效通信：

1. **协议签名验证**：
   - 逆向重构了通讯链路的散列验签机制：`MD5(nTimeStamp + strRandom + strUser + strAppID + strToken)`。
   - 采用标准 Base64 封包，实现轻量级 `RPC` 调用，免去解析多层复杂 HTTP 资源的开销。
2. **硬件电量精确解码**：
   - 逆向分析硬件状态字 `nTEState`：通过按位运算 `((teState and 0x00FF0000L) shr 16)` 实时提取硬件内置电池的 0~100% 精确电量，杜绝官方前端虚标。
3. **坐标系双向转换**：
   - 定位器硬件上报原生 **WGS-84 (GPS)** 经纬度。
   - 内置自主实现的高精度坐标算法（无任何第三方网络依赖），提供 **WGS-84 ⇄ GCJ-02 (火星坐标系)** 双向转换，无缝与国内主流地图呈现及电子围栏对齐。
4. **工作模式调度**：
   - 完整支持设备的 5 个工作模式切换（超长待机、智能省电、高频定位等）。

---

## 📱 核心功能模块

- **设备看板**：实时查看设备在线状态、精准电量百分比、当前速度、卫星定位更新时间及逆地理编码位置。
- **实时追踪**：直观展示目标动态移动轨迹与火星坐标数据。
- **历史轨迹回放**：拉取长达 48 小时的历史轨迹点，支持播放、暂停、倍速回放与进度条快速拖动。
- **电子围栏管理**：支持圆形/多边形围栏查询与出入报警配置。
- **报警通知**：位移震动报警、出入围栏报警、超速报警及低电量预警。

---

## 🚀 编译与构建

### 1. 本地编译要求
- **Android Studio**：Ladybug (2024.2.1) 或更新版本
- **JDK**：OpenJDK 17 或 21
- **Android SDK**：API Level 36 (Android 16 预览版或最新预览工具链)
- **Gradle**：9.7.0 (项目内含 `gradlew` 包装器)

### 2. 命令行构建
```bash
# 克隆仓库
git clone https://github.com/<your-username>/NewmanGPS.git
cd NewmanGPS

# 构建 Debug 版本 APK
./gradlew assembleDebug

# 构建 Release 签名版本 APK
./gradlew assembleCiRelease
```
生成的 APK 文件将位于 `app/build/outputs/apk/` 目录下。

---

## 🤖 GitHub Actions 自动编译与发版

本项目已预配置全自动化的 GitHub Actions CI/CD 工作流（`.github/workflows/android-release.yml`）。

### 1. 配置密钥 (Secrets)
若需要在打 Tag 时自动发布已签名的 Release APK，请在 GitHub 仓库的 **Settings -> Secrets and variables -> Actions** 中添加以下密钥：

| Secret 名称 | 说明 |
| :--- | :--- |
| `SIGNING_STORE_BASE64` | Android 签名密钥库文件（`.jks` / `.keystore` / `.p12`）的 Base64 编码字符串 |
| `ANDROID_KEYSTORE_PASSWORD` | 密钥库保护密码 |
| `ANDROID_KEY_ALIAS` | 密钥别名 (Key Alias) |
| `ANDROID_KEY_PASSWORD` | 密钥私钥密码 |

> **提示**：生成 `SIGNING_STORE_BASE64` 可以使用以下命令：
> ```bash
> # Linux / macOS
> base64 -w 0 your-keystore.jks > keystore-base64.txt
> 
> # Windows PowerShell
> [Convert]::ToBase64String([IO.File]::ReadAllBytes("your-keystore.jks")) | Out-File -Encoding utf8 keystore-base64.txt
> ```

### 2. 触发自动发布
只需向主仓库推送版本标签（例如 `v1.0.0`），GitHub Actions 将自动执行编译、签名、对齐优化并创建 GitHub Release：
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## ⚠️ 免责声明

1. 本项目为开源第三方客户端，仅供个人技术研究、学习逆向工程及原生 Android / Jetpack Compose 架构开发使用。
2. 本项目与“纽曼”（Newsmy）官方无任何商业关联或授权关系。
3. 请在遵守当地法律法规及设备用户协议的前提下合理使用。
