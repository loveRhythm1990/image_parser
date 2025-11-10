# 快速打包指南 🚀

## 📱 打包 APK 的三种方式

---

## 方式一：Android Studio - Build APK（最简单）⭐

### Debug 版本（用于测试）

1. 在 Android Studio 中打开项目
2. 点击菜单：**Build > Build Bundle(s) / APK(s) > Build APK(s)**
3. 等待构建完成（1-3分钟）
4. 看到提示后点击 **locate** 查看 APK

**输出位置**：
```
/Users/lr90/andro/app/build/outputs/apk/debug/app-debug.apk
```

✅ 可以直接安装到手机测试！

---

## 方式二：Android Studio - Generate Signed APK（用于发布）

### Release 版本（已签名，可发布）

1. 点击菜单：**Build > Generate Signed Bundle / APK**
2. 选择 **APK**，点击 **Next**
3. **创建新密钥**（如果是第一次）：
   - 点击 **Create new...**
   - 选择密钥存储路径（例如：`~/floating-screenshot.keystore`）
   - 输入密码和别名信息
   - **保存好密钥文件和密码！**
4. 选择构建变体：**release**
5. 点击 **Finish**

**输出位置**：
```
/Users/lr90/andro/app/release/app-release.apk
```

✅ 这个版本可以上传到应用市场或分享给用户！

---

## 方式三：命令行（需要先配置 Gradle Wrapper）

### 前置条件

确保项目有完整的 Gradle Wrapper。如果没有，在项目根目录运行：

```bash
# 如果系统安装了 Gradle
gradle wrapper --gradle-version 8.2
```

或者使用 Android Studio 的 Terminal：

```bash
# 使用 Android Studio 内置的 Gradle
./gradlew wrapper --gradle-version 8.2
```

### 构建命令

```bash
# 进入项目目录
cd /Users/lr90/andro

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需要先配置签名）
./gradlew assembleRelease

# 清理后重新构建
./gradlew clean assembleDebug
```

**输出位置**：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## 📊 Debug vs Release 对比

| 特性 | Debug | Release |
|------|-------|---------|
| 用途 | 开发测试 | 正式发布 |
| 签名 | 自动签名 | 需要自定义签名 |
| 体积 | 较大 | 较小（可混淆） |
| 性能 | 包含调试信息 | 优化过 |
| 安装 | 覆盖安装需同签名 | 需要卸载 Debug 版 |

---

## 🔧 常见问题

### Q: 构建失败，提示 Gradle sync failed？
**A**: 在 Android Studio 中：
1. 点击 `File > Sync Project with Gradle Files`
2. 等待同步完成后重新构建

### Q: APK 安装时提示"解析包时出现问题"？
**A**: 可能原因：
- APK 损坏（重新构建）
- 签名问题（卸载旧版本后重装）
- 最低 SDK 版本不匹配（本应用需要 Android 8.0+）

### Q: 如何缩小 APK 体积？
**A**: 在 `app/build.gradle` 的 release 配置中：
```gradle
buildTypes {
    release {
        minifyEnabled true  // 启用代码混淆和优化
        shrinkResources true  // 移除未使用的资源
    }
}
```

### Q: 忘记了签名密钥密码怎么办？
**A**: 签名密钥无法恢复！需要：
1. 重新生成新的密钥
2. 用新密钥签名的 APK 无法覆盖旧版本
3. 用户需要卸载旧版本后安装新版本

---

## 📝 推荐工作流程

1. **开发阶段**：使用 Debug 版本快速迭代测试
2. **内部测试**：分享 Debug APK 给测试人员
3. **准备发布**：
   - 创建签名密钥（只需一次）
   - 构建 Release APK
   - 在多个设备上测试
   - 上传到应用市场或分享给用户

---

## 🎯 推荐方式

**新手或快速测试**：👉 使用 **方式一**（Android Studio - Build APK）

**准备发布应用**：👉 使用 **方式二**（Generate Signed APK）

**自动化构建**：👉 使用 **方式三**（命令行）

---

祝您打包顺利！🎉

