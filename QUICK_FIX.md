# 快速修复指南

## 问题：截屏后英雄名字没有更新

### 最快的诊断方法

打开 Android Studio 的 Logcat，执行以下操作：

#### 1. 清空日志
```
点击 Logcat 窗口的 "Clear logcat" 按钮（垃圾桶图标）
```

#### 2. 设置过滤器
在 Logcat 的搜索框中输入：
```
FloatingWindow|ScreenshotObserver|HeroRecognition
```

#### 3. 进行截图
使用系统截图功能截取屏幕

#### 4. 观察日志

你应该能看到类似以下的日志输出。请告诉我你看到了哪些，没看到哪些：

**✅ 应该看到：**
```
D/ScreenshotObserver: 检测到媒体库更新: id=xxx, name=Screenshot_xxx.png
D/ScreenshotObserver: 已将截图复制到缓存: /data/user/0/.../cache/Screenshot_xxx.png
D/FloatingWindow: 检测到系统截图: /xxx/Screenshot_xxx.png
D/FloatingWindow: ========== 开始上传截图进行识别 ==========
D/FloatingWindow: 截图路径: /xxx/Screenshot_xxx.png
D/FloatingWindow: 文件是否存在: true
D/FloatingWindow: 文件大小: 123456 bytes
D/HeroRecognitionService: 开始识别英雄，图片路径: xxx
```

**然后等待（可能需要1-2分钟），应该看到：**
```
D/HeroRecognitionService: 收到服务器响应，状态码: 200
D/HeroRecognitionService: 服务器响应内容: {"heroes":["xxx","yyy","zzz","aaa","bbb"],...}
D/FloatingWindow: 识别成功: [xxx, yyy, zzz, aaa, bbb]
```

**或者看到错误：**
```
E/HeroRecognitionService: 网络请求失败: xxxxx
E/FloatingWindow: 识别失败: xxxxx
```

---

## 根据日志情况的解决方案

### 情况 A：完全没有看到任何日志

**原因：** 悬浮窗服务没有启动或截图监听没有开启

**解决方法：**
1. 关闭并重新打开 app
2. 点击"启动悬浮窗"按钮
3. 查看是否有悬浮按钮出现在屏幕上
4. 重新截图测试

---

### 情况 B：看到"检测到媒体库更新"但没有后续

**原因：** 截图路径不符合条件（不在 Screenshots 目录）

**Logcat 中查找：**
```
D/ScreenshotObserver: 更新的图片路径不在截图目录，忽略
```

**解决方法：**
- 确保使用系统自带的截图功能（通常是 电源键+音量下键）
- 不要使用第三方截图 app
- 检查截图是否保存在 DCIM/Screenshots 目录

---

### 情况 C：看到"开始上传截图"但文件不存在

**Logcat 显示：**
```
D/FloatingWindow: 文件是否存在: false
E/HeroRecognitionService: 图片文件不存在: xxx
```

**原因：** Android 10+ 文件访问权限问题

**解决方法：**
1. 检查是否授予了"照片和视频"权限
2. 卸载 app 并重新安装
3. 重新授予所有权限

---

### 情况 D：看到"开始上传截图"但一直没有响应

**Logcat 显示：**
```
D/FloatingWindow: ========== 开始上传截图进行识别 ==========
D/HeroRecognitionService: 开始识别英雄，图片路径: xxx
（然后就没有后续了）
```

**原因：** 网络请求卡住或服务器无响应

**可能的子原因：**
1. **网络连接问题**
   - 手机没有联网
   - WiFi/移动网络不稳定
   - 防火墙阻止了连接

2. **服务器问题**
   - 服务器地址不可达
   - 服务器处理超时

**解决方法：**

#### 方法1：测试网络连接
在手机浏览器中访问：
```
http://shanghai.idc.matrixorigin.cn:30026/health
```

如果无法访问，说明网络或服务器有问题。

#### 方法2：检查服务器状态
在电脑上运行：
```bash
curl http://shanghai.idc.matrixorigin.cn:30026/health
```

应该返回：
```json
{"status":"healthy","service":"hero-recognition-api"}
```

#### 方法3：使用 adb 查看网络请求
```bash
adb shell "am start -a android.intent.action.VIEW -d http://shanghai.idc.matrixorigin.cn:30026/health"
```

---

### 情况 E：看到"识别成功"但英雄名字没更新

**Logcat 显示：**
```
D/FloatingWindow: 识别成功: [英雄1, 英雄2, ...]
D/FloatingWindow: 更新后的英雄列表: [英雄1, 英雄2, ...]
```

**但是：** 没有看到 "更新英雄名字" 的日志

**原因：** 面板可能还没显示，或者 UI 更新在面板显示之前

**解决方法：**

这是一个 **时序问题**。我需要修改代码，确保面板显示后再更新英雄名字。

---

### 情况 F：看到网络请求失败

**Logcat 显示：**
```
E/HeroRecognitionService: 网络请求失败: Unable to resolve host "shanghai.idc.matrixorigin.cn"
```

**原因：** DNS 解析失败

**解决方法：**
1. 检查手机网络连接
2. 尝试切换 WiFi/移动网络
3. 检查是否使用了 VPN（可能干扰连接）

---

### 情况 G：服务器返回 4xx/5xx 错误

**Logcat 显示：**
```
E/HeroRecognitionService: 服务器返回错误: 400/500, 错误详情: xxx
```

**原因：** 服务器端问题

**解决方法：**
1. 检查服务器日志
2. 确认服务器是否正常运行
3. 检查图片格式是否正确

---

## 临时解决方案：手动测试

如果网络确实有问题，可以先测试本地功能：

### 1. 修改代码使用模拟数据

我可以为你创建一个测试版本，跳过网络请求，直接使用模拟的英雄数据来验证 UI 更新是否正常。

---

## 请执行以下操作并反馈结果

1. ✅ 打开 Android Studio Logcat
2. ✅ 清空日志
3. ✅ 设置过滤器：`FloatingWindow|ScreenshotObserver|HeroRecognition`
4. ✅ 进行截图
5. ✅ 等待 2 分钟
6. ✅ 复制 Logcat 中的所有日志
7. ✅ 告诉我你看到了什么（或完全没看到）

这样我就能准确定位问题了！

