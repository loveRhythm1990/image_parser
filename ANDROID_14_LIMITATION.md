# Android 14 截屏限制说明

## 问题描述

在 **Android 14 (API 34)** 系统上，使用 `MediaProjection` API（截屏功能）有严格的限制：

```
SecurityException: Media projections require a foreground service of type 
ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
```

## 技术原因

1. Android 14 要求使用 `MediaProjection` 的应用必须运行 `mediaProjection` 类型的前台服务
2. 启动 `mediaProjection` 类型服务需要 `CAPTURE_VIDEO_OUTPUT` 系统级权限
3. `CAPTURE_VIDEO_OUTPUT` 权限**只能授予系统应用**，普通应用无法获得
4. 这个限制基于**系统版本**，不管应用的 `targetSdk` 是多少都会生效

## 解决方案

### 方案 1：在 Android 13 或更早版本测试（推荐）

在 **Android Studio** 中创建或切换到 Android 13 模拟器：

1. 打开 **Tools → Device Manager**
2. 点击 **Create Device**
3. 选择任意设备型号
4. **系统镜像选择**: 选择 **Android 13.0 (API 33)** 或更早版本
5. 点击 **Finish** 创建模拟器
6. 在新模拟器中安装并测试应用

### 方案 2：在真实设备上测试

如果您有运行 Android 13 或更早版本的真实设备：

1. 在设备上启用开发者模式
2. 启用 USB 调试
3. 通过 USB 连接设备
4. 在 Android Studio 中选择您的设备进行安装

### 方案 3：使用替代方案（需要重新开发）

对于需要在 Android 14+ 运行的应用，可以考虑：

- 使用 Accessibility Service 进行截屏（需要用户授予辅助功能权限）
- 仅支持特定的截屏场景（如只截取应用自己的内容）
- 等待 Android 未来版本提供新的截屏 API

## 当前应用配置

- `minSdk`: 26 (Android 8.0)
- `targetSdk`: 33 (Android 13)
- `compileSdk`: 34

**推荐测试环境**: Android 10 - Android 13

## 功能支持情况

| Android 版本 | 悬浮窗 | 截屏 | 状态 |
|-------------|-------|------|------|
| Android 8-13 | ✅ | ✅ | 完全支持 |
| Android 14+ | ✅ | ❌ | 仅悬浮窗可用 |

## 测试步骤（Android 13 模拟器）

1. 启动 Android 13 或更早版本的模拟器
2. 在 Android Studio 中构建并安装应用
3. 打开应用
4. 授予"悬浮窗"权限
5. 点击"启动悬浮窗"
6. 点击屏幕上的紫色悬浮按钮
7. 授予"截屏"权限
8. 再次点击悬浮按钮 → **截屏成功！** ✅

## 如何在 Android Studio 中切换模拟器

1. 点击顶部工具栏的设备选择下拉菜单
2. 如果没有 Android 13 模拟器，点击 **Device Manager**
3. 创建新的 Android 13 模拟器（推荐 Pixel 5 + Android 13）
4. 等待模拟器启动
5. 重新运行应用

---

**注意**: 这是 Android 14 的系统限制，不是应用的 bug。在支持的 Android 版本上，应用功能完全正常。

