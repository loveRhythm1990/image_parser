# 如何构建测试版 APK（不需要签名）

## ✅ 推荐方式：Build APK（无需密码）

### 步骤 1：清理项目
在 Android Studio 中点击：
```
Build > Clean Project
```

### 步骤 2：构建 Debug APK
点击菜单：
```
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

**注意**：是 "Build APK(s)"，**不是** "Generate Signed Bundle / APK"

### 步骤 3：等待构建完成
- 底部会显示构建进度
- 构建成功后会弹出通知："APK(s) generated successfully"

### 步骤 4：获取 APK
点击通知中的 **"locate"** 链接，或手动打开：
```
/Users/lr90/andro/app/build/outputs/apk/debug/app-debug.apk
```

---

## ❌ 不要选择这个（用于正式发布）

**Generate Signed Bundle / APK** 
- ❌ 需要创建签名密钥（Keystore）
- ❌ 需要输入密码
- ❌ 用于发布到应用商店
- ❌ 测试阶段不需要

---

## 🔧 如果 Build APK 也要求密码

### 可能原因 1：Mac 钥匙串访问
这是 macOS 系统的安全提示，允许即可。

### 可能原因 2：项目配置了签名
检查 `app/build.gradle` 文件，确保 debug 构建类型没有配置签名：

```gradle
buildTypes {
    debug {
        // 不要有 signingConfig 配置
    }
    release {
        minifyEnabled false
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

---

## 💻 命令行构建（备选方案）

如果 Android Studio 一直要求密码，可以使用命令行：

```bash
cd /Users/lr90/andro

# 清理项目
./gradlew clean

# 构建 Debug APK
./gradlew assembleDebug
```

APK 会生成在：
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📝 总结

**测试阶段**：
- ✅ 使用 `Build > Build APK(s)`
- ✅ 生成 `app-debug.apk`
- ✅ 无需密码和签名

**发布阶段**：
- 📦 使用 `Generate Signed Bundle / APK`
- 📦 需要创建签名密钥
- 📦 需要保管好密码

