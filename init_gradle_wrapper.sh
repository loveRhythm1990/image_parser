#!/bin/bash

# 初始化 Gradle Wrapper 脚本
# 如果您的系统没有安装 Gradle，可以使用 Android Studio 的 Terminal 运行此脚本

echo "🔧 正在初始化 Gradle Wrapper..."
echo ""

# 检查是否在正确的目录
if [ ! -f "settings.gradle" ]; then
    echo "❌ 错误：请在项目根目录运行此脚本"
    echo "   当前目录：$(pwd)"
    echo "   应该在：/Users/lr90/andro"
    exit 1
fi

# 创建 wrapper 目录
mkdir -p gradle/wrapper

# 下载 gradle-wrapper.jar
echo "📥 正在下载 Gradle Wrapper JAR..."
WRAPPER_URL="https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar"
curl -L -o gradle/wrapper/gradle-wrapper.jar "$WRAPPER_URL"

if [ $? -ne 0 ]; then
    echo "❌ 下载失败！"
    echo "   请检查网络连接，或在 Android Studio 中使用 Terminal"
    exit 1
fi

# 确保 gradlew 有执行权限
chmod +x gradlew

echo ""
echo "✅ Gradle Wrapper 初始化完成！"
echo ""
echo "现在您可以运行："
echo "  ./gradlew assembleDebug    # 构建 Debug APK"
echo "  ./gradlew assembleRelease  # 构建 Release APK"
echo ""

