# 🐛 应用调试指南

## 📱 问题 1：查看崩溃日志

### 方法 1：在 Android Studio 中查看 Logcat（推荐）

1. **打开 Logcat 窗口**
   - 在 Android Studio 底部点击 **`Logcat`** 标签
   - 或使用快捷键：`Cmd + 6` (Mac) / `Alt + 6` (Windows)

2. **过滤日志**
   - 在搜索框中输入：`FloatingWindow`
   - 或输入：`AndroidRuntime` 查看崩溃信息

3. **运行应用并观察日志**
   - 启动应用
   - 点击"授权并启动"按钮
   - 查看 Logcat 中的输出

**关键日志信息：**
```
D/FloatingWindow: Service onCreate 被调用
D/FloatingWindow: Service onStartCommand 被调用
D/FloatingWindow: 悬浮窗已添加到屏幕
```

如果看到这些日志，说明服务正常启动。

如果看到错误日志：
```
E/FloatingWindow: 创建悬浮窗失败: xxx
E/AndroidRuntime: FATAL EXCEPTION: xxx
```

---

### 方法 2：使用终端命令（备选）

如果 Android Studio 的 Logcat 不工作，可以在终端中运行：

```bash
# Mac/Linux 系统（假设 adb 已配置到 PATH）
adb logcat | grep -E "FloatingWindow|AndroidRuntime"

# 如果 adb 不在 PATH 中，使用完整路径
~/Library/Android/sdk/platform-tools/adb logcat | grep -E "FloatingWindow|AndroidRuntime"

# 清除日志并重新开始
~/Library/Android/sdk/platform-tools/adb logcat -c
~/Library/Android/sdk/platform-tools/adb logcat | grep -E "FloatingWindow|AndroidRuntime"
```

---

### 方法 3：保存完整日志到文件

```bash
# 保存日志到文件
~/Library/Android/sdk/platform-tools/adb logcat > ~/Desktop/app_log.txt

# 等待应用崩溃后，按 Ctrl+C 停止
# 然后打开 ~/Desktop/app_log.txt 查看
```

---

## 🔘 问题 2：为什么看不到悬浮按钮？

### 可能的原因和解决方案：

#### 原因 1：没有授予悬浮窗权限 ✅

**解决方案：**

1. 打开模拟器的 **Settings（设置）**
2. 进入 **Apps → 悬浮截屏**
3. 找到 **Display over other apps**（在其他应用上层显示）
4. 打开这个开关

或者在应用中：
- 点击"授权并启动"按钮
- 点击"前往设置"
- 开启权限后返回

---

#### 原因 2：服务没有正常启动 ❌

**检查方法：**

在 Logcat 中查找：
```
D/FloatingWindow: Service onCreate 被调用
D/FloatingWindow: Service onStartCommand 被调用
```

如果没有这些日志，说明服务没启动。

**解决方案：**
- 确保点击了"启动悬浮窗"按钮
- 检查是否有权限拒绝的错误

---

#### 原因 3：布局文件问题 ❌

**检查方法：**

查看是否有类似错误：
```
E/FloatingWindow: 创建悬浮窗失败: xxx
```

**解决方案：**
- 查看详细错误信息
- 检查布局文件 `floating_window.xml` 是否正确

---

#### 原因 4：悬浮按钮太小或透明度太高 ❌

**解决方案：**
- 检查悬浮按钮的大小（应该是 64x64 dp）
- 检查透明度设置（alpha 应该是 0.8，不要太透明）

---

## 📸 问题 3：点击悬浮按钮后截屏功能

### 预期行为：

1. **点击悬浮按钮**
   - 应该看到 Toast 提示："准备截屏..."
   - Logcat 显示：`D/FloatingWindow: 悬浮按钮被点击`

2. **首次点击**
   - 会弹出系统权限对话框
   - 内容："开始投射屏幕"
   - 点击"立即开始"授权

3. **授权后**
   - 自动截屏
   - 显示 Toast："截屏已保存: /path/to/file"

4. **后续点击**
   - 直接截屏，不再需要授权

---

### 如果没有反应：

**检查 Logcat：**
```
# 点击按钮后应该看到：
D/FloatingWindow: 悬浮按钮被点击

# 如果没有这条日志，说明点击事件没有触发
```

**可能的原因：**
1. 按钮太小，点击区域不准确
2. Touch 事件被拖动逻辑拦截了

---

## 🎯 完整测试流程

### 步骤 1：重新构建并安装

```bash
# 在 Android Studio 中
Build > Clean Project
Build > Build APK(s)

# 卸载旧版本
adb uninstall com.example.floatingscreenshot

# 安装新版本
# 将 APK 拖入模拟器
```

### 步骤 2：打开 Logcat

在 Android Studio 底部点击 **Logcat** 标签

### 步骤 3：启动应用并观察

1. **打开应用**
   ```
   期待日志：应用启动
   ```

2. **点击"授权并启动"**
   ```
   期待日志：
   D/FloatingWindow: Service onCreate 被调用
   D/FloatingWindow: Service onStartCommand 被调用
   ```

3. **如果弹出授权对话框，点击"前往设置"并授权**
   ```
   期待日志：无
   ```

4. **返回应用，再次点击"启动悬浮窗"**
   ```
   期待日志：
   D/FloatingWindow: 悬浮窗已添加到屏幕
   期待 Toast："悬浮按钮已显示"
   ```

5. **在屏幕上寻找悬浮按钮**
   ```
   位置：屏幕左上角偏右一点（x=100, y=100）
   外观：半透明圆形按钮，内有图标
   ```

6. **点击悬浮按钮**
   ```
   期待日志：
   D/FloatingWindow: 悬浮按钮被点击
   期待 Toast："准备截屏..."
   ```

7. **首次会弹出截屏权限对话框**
   ```
   点击"立即开始"
   ```

8. **截屏成功**
   ```
   期待 Toast："截屏已保存: /path/to/file"
   ```

---

## ❌ 常见错误及解决方案

### 错误 1：Permission Denial
```
java.lang.SecurityException: Permission Denial: 
requires android.permission.SYSTEM_ALERT_WINDOW
```

**解决方案：**
- 手动授予悬浮窗权限
- 设置 > 应用 > 悬浮截屏 > Display over other apps > 开启

---

### 错误 2：View not attached to window manager
```
java.lang.IllegalArgumentException: 
View not attached to window manager
```

**解决方案：**
- 这是停止服务时的错误
- 已在代码中添加了 try-catch 处理

---

### 错误 3：Service not registered
```
java.lang.IllegalArgumentException: 
Service not registered
```

**解决方案：**
- 检查 AndroidManifest.xml 中是否正确声明了 Service

---

## 📞 如何报告问题

如果问题仍然存在，请提供以下信息：

1. **Logcat 日志**
   - 从启动应用到崩溃的完整日志
   - 特别是包含 "FloatingWindow" 或 "AndroidRuntime" 的行

2. **操作步骤**
   - 您具体点击了什么
   - 看到了什么现象
   - 预期应该是什么

3. **系统信息**
   - Android 版本
   - 是真机还是模拟器

---

## ✅ 成功的标志

如果一切正常，您应该看到：

1. ✅ 应用可以打开，没有崩溃
2. ✅ 点击"授权并启动"后能看到 Toast："悬浮按钮已显示"
3. ✅ 屏幕上出现半透明圆形悬浮按钮
4. ✅ 可以拖动悬浮按钮改变位置
5. ✅ 点击悬浮按钮能看到 Toast："准备截屏..."
6. ✅ 首次点击会请求截屏权限
7. ✅ 授权后能成功截屏并保存

祝调试顺利！🎉

