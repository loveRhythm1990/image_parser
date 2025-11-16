# 悬浮截屏应用

一个简单的 Android 应用，提供悬浮窗快速截屏功能。

## 功能特性

- ✨ **悬浮窗按钮**：可以悬浮在其他应用上方
- 🎨 **半透明设计**：悬浮窗采用 50% 透明效果，不遮挡视线
- 📸 **一键截屏**：点击悬浮按钮即可快速截屏
- 🔄 **可拖动**：悬浮窗位置可以自由拖动
- 💾 **自动保存**：截屏自动保存到下载目录，方便查看

## 系统要求

- Android 8.0 (API 26) 或更高版本
- 需要授予悬浮窗权限和截屏权限

## 项目结构

```
andro/
├── app/
│   ├── build.gradle                    # 应用级 Gradle 配置
│   ├── proguard-rules.pro             # ProGuard 规则
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml     # 应用清单文件
│           ├── java/com/example/floatingscreenshot/
│           │   ├── MainActivity.kt                      # 主界面
│           │   ├── FloatingWindowService.kt            # 悬浮窗服务
│           │   ├── ScreenshotManager.kt                # 截屏管理器
│           │   └── ScreenshotPermissionActivity.kt     # 截屏权限请求
│           └── res/
│               ├── drawable/
│               │   └── ic_screenshot.xml               # 截屏图标
│               ├── layout/
│               │   ├── activity_main.xml               # 主界面布局
│               │   └── floating_window.xml             # 悬浮窗布局
│               └── values/
│                   ├── colors.xml                      # 颜色资源
│                   ├── strings.xml                     # 字符串资源
│                   └── themes.xml                      # 主题样式
├── build.gradle                        # 项目级 Gradle 配置
├── settings.gradle                     # Gradle 设置
└── gradle.properties                   # Gradle 属性

```

## 如何构建

### 前提条件

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17 或更高版本
- Android SDK (通过 Android Studio 安装)

### 使用 Android Studio

1. **打开项目**
   - 启动 Android Studio
   - 选择 `File > Open`
   - 导航到 `/Users/lr90/andro` 目录并打开

2. **配置 SDK 路径**（如果需要）
   - Android Studio 会自动检测 SDK 路径
   - 如果提示找不到 SDK，请在 `File > Project Structure > SDK Location` 中设置

3. **等待 Gradle 同步**
   - 首次打开项目时，Gradle 会自动同步并下载依赖
   - 这可能需要几分钟时间（取决于网络速度）
   - 如果同步失败，点击 `File > Sync Project with Gradle Files` 重试

4. **连接设备或启动模拟器**
   - 连接 Android 手机（需开启 USB 调试）
   - 或者在 `Tools > Device Manager` 中创建/启动模拟器

5. **运行应用**
   - 点击工具栏的 `Run` 按钮（绿色三角形）
   - 或按快捷键 `Shift + F10` (Mac) / `Shift + F10` (Windows)
   - 选择目标设备
   - 等待编译和安装完成

### 使用命令行

**注意**：首次使用命令行构建前，需要初始化 Gradle Wrapper：

```bash
# 方式 1：使用提供的初始化脚本（需要网络）
cd /Users/lr90/andro
./init_gradle_wrapper.sh

# 方式 2：或在 Android Studio 的 Terminal 中运行
./gradlew wrapper
```

初始化完成后，就可以使用命令行构建了：

```bash
# 构建 Debug APK（用于测试）
./gradlew assembleDebug

# 构建 Release APK（需要先配置签名）
./gradlew assembleRelease

# 构建并安装到连接的设备
./gradlew installDebug

# 清理后重新构建
./gradlew clean assembleDebug
```

**生成的 APK 文件位置**：
- Debug 版本：`app/build/outputs/apk/debug/app-debug.apk`
- Release 版本：`app/build/outputs/apk/release/app-release.apk`

---

## 📦 打包 APK

详细的打包指南请查看：**[QUICK_BUILD_GUIDE.md](QUICK_BUILD_GUIDE.md)**

### 快速打包（Android Studio）

1. **Debug 版本**（快速测试）
   - 点击 `Build > Build Bundle(s) / APK(s) > Build APK(s)`
   - APK 生成在 `app/build/outputs/apk/debug/`

2. **Release 版本**（正式发布）
   - 点击 `Build > Generate Signed Bundle / APK`
   - 选择 `APK` > 创建或选择签名密钥
   - APK 生成在 `app/release/`

📖 **详细说明**：
- Debug 打包：查看 [QUICK_BUILD_GUIDE.md](QUICK_BUILD_GUIDE.md)
- Release 签名：查看 [keystore_guide.md](app/keystore_guide.md)

## 使用说明

1. **首次启动**
   - 打开应用后，系统会提示授予"在其他应用上显示"的权限
   - 点击"前往设置"，开启悬浮窗权限

2. **启动悬浮窗**
   - 返回应用，点击"启动悬浮窗"按钮
   - 悬浮按钮会出现在屏幕上

3. **使用截屏**
   - 点击悬浮按钮
   - 首次使用时，系统会请求截屏权限，点击"立即开始"
   - 截屏会自动保存到图片文件夹

4. **移动悬浮窗**
   - 长按并拖动悬浮按钮可以改变其位置

5. **停止悬浮窗**
   - 返回应用主界面
   - 点击"停止悬浮窗"按钮

## 权限说明

应用需要以下权限：

- **SYSTEM_ALERT_WINDOW**：在其他应用上显示悬浮窗
- **FOREGROUND_SERVICE**：保持悬浮窗服务运行
- **FOREGROUND_SERVICE_MEDIA_PROJECTION**：用于截屏功能
- **READ_MEDIA_IMAGES**：访问保存的截屏（Android 13+）

## 技术实现

- **语言**：Kotlin
- **最低 SDK**：26 (Android 8.0)
- **目标 SDK**：34 (Android 14)
- **架构组件**：
  - WindowManager：管理悬浮窗
  - MediaProjection API：实现截屏
  - Foreground Service：保持服务运行
  - ViewBinding：视图绑定

## 截屏保存位置

- **Android 10+**：`/sdcard/Download/`
- **Android 9-**：`/sdcard/Download/`

文件名格式：`Screenshot_yyyyMMdd_HHmmss.png`

## 常见问题

### 构建相关

#### Q: Gradle 同步失败？
A: 常见解决方案：
1. 检查网络连接（Gradle 需要下载依赖）
2. 在 Android Studio 中点击 `File > Invalidate Caches / Restart`
3. 删除项目根目录的 `.gradle` 文件夹后重新同步
4. 确保使用的是 JDK 17 或更高版本

#### Q: 提示找不到 SDK？
A: 
1. 打开 `File > Project Structure > SDK Location`
2. 设置 Android SDK 路径（通常在 `~/Library/Android/sdk` (Mac) 或 `C:\Users\用户名\AppData\Local\Android\Sdk` (Windows)）
3. 或者在 `local.properties` 文件中手动设置：`sdk.dir=/path/to/android/sdk`

#### Q: 编译错误：Could not find com.android.tools.build:gradle:xxx
A: 这是网络问题，请：
1. 检查 VPN 或代理设置
2. 等待一段时间后重试
3. 或在 `gradle.properties` 中配置镜像源

### 应用使用相关

#### Q: 悬浮窗没有显示？
A: 请检查是否授予了"在其他应用上显示"的权限。进入系统设置 > 应用 > 悬浮截屏 > 在其他应用上显示，确保已开启。

#### Q: 截屏失败？
A: 首次点击截屏按钮时，需要授予截屏权限。如果拒绝了权限，需要重新启动应用并授权。

#### Q: 找不到截屏图片？
A: 截屏保存在下载目录。可以使用文件管理器查看，或在截屏成功时查看 Toast 提示的完整路径。

## 开发建议

如需进一步开发，可以考虑：

1. 添加更多截屏选项（延时截屏、区域截屏等）
2. 支持截屏编辑功能
3. 添加截屏历史记录
4. 优化悬浮窗样式和动画
5. 支持自定义保存位置
6. 添加分享功能

## 许可证

本项目仅供学习参考使用。

## 注意事项

⚠️ **重要提示**：
- 截屏功能涉及隐私，请遵守相关法律法规
- 不要在未经用户同意的情况下截取敏感信息
- 本应用需要较高的系统权限，请确保从可信来源安装

---

开发完成 ✅

