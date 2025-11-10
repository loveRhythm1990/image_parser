# Release 版本打包指南

## 第一步：生成签名密钥

在终端中运行（将路径替换为您想要保存密钥的位置）：

```bash
keytool -genkey -v -keystore ~/floating-screenshot.keystore \
        -alias floating_screenshot \
        -keyalg RSA -keysize 2048 -validity 10000
```

按提示输入：
- 密钥库密码（输入两次）
- 姓名、组织等信息
- 密钥密码（可以与密钥库密码相同）

**重要**：请妥善保管密钥文件和密码！

## 第二步：配置签名

在 `app/build.gradle` 中添加签名配置（在 android {} 块中）：

```gradle
android {
    ...
    
    signingConfigs {
        release {
            storeFile file("你的密钥文件路径/floating-screenshot.keystore")
            storePassword "你的密钥库密码"
            keyAlias "floating_screenshot"
            keyPassword "你的密钥密码"
        }
    }
    
    buildTypes {
        release {
            minifyEnabled false
            signingConfig signingConfigs.release
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

## 第三步：构建 Release APK

### 方法 1：使用 Android Studio
1. 点击 `Build > Generate Signed Bundle / APK`
2. 选择 `APK`，点击 `Next`
3. 选择密钥文件，输入密码
4. 选择 `release` 构建类型
5. 点击 `Finish`

### 方法 2：使用命令行
```bash
cd /Users/lr90/andro
./gradlew assembleRelease
```

生成的 APK 在：`app/build/outputs/apk/release/app-release.apk`

