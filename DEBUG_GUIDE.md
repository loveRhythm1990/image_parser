# 英雄识别功能调试指南

## 问题：截屏后英雄名字没有更新

如果你截屏后等了2分钟，英雄1-5的按钮还没有被替换为英雄名字，请按以下步骤排查：

## 步骤1：查看 Logcat 日志

在 Android Studio 中打开 Logcat，过滤关键字查看日志：

### 1.1 检查截图是否被检测到
过滤关键字：`ScreenshotObserver` 或 `FloatingWindow`

**期望看到的日志：**
```
ScreenshotObserver: 检测到媒体库更新: id=xxx, name=Screenshot_xxx.png, bucket=Screenshots...
ScreenshotObserver: 已将截图复制到缓存: /data/user/0/.../cache/Screenshot_xxx.png
FloatingWindow: 检测到系统截图: /path/to/screenshot.png
FloatingWindow: ========== 开始上传截图进行识别 ==========
FloatingWindow: 截图路径: /path/to/screenshot.png
FloatingWindow: 文件是否存在: true
FloatingWindow: 文件大小: xxxxx bytes
```

**如果没有看到这些日志：**
- 问题：截图没有被检测到
- 解决：检查是否授予了存储权限（READ_MEDIA_IMAGES 或 READ_EXTERNAL_STORAGE）

### 1.2 检查网络请求是否发送
过滤关键字：`HeroRecognitionService`

**期望看到的日志：**
```
HeroRecognitionService: 开始识别英雄，图片路径: xxx
HeroRecognitionService: 收到服务器响应，状态码: 200
HeroRecognitionService: 服务器响应内容: {"heroes":["xxx","yyy",...]}
```

**如果看到网络请求失败：**
- `网络请求失败: Unable to resolve host`: DNS 解析失败，检查网络连接
- `网络请求失败: timeout`: 超时（不应该发生，我们设置了无超时）
- `服务器返回错误: 4xx/5xx`: 服务器问题

### 1.3 检查识别结果
过滤关键字：`FloatingWindow`

**期望看到的日志：**
```
FloatingWindow: 识别成功: [英雄1, 英雄2, 英雄3, 英雄4, 英雄5]
FloatingWindow: 更新后的英雄列表: [英雄1, 英雄2, 英雄3, 英雄4, 英雄5]
FloatingWindow: 更新英雄名字 1: 英雄1
FloatingWindow: 更新英雄名字 2: 英雄2
...
```

## 步骤2：检查权限

### 2.1 网络权限
打开 `设置 > 应用 > 悬浮截屏 > 权限`，确保：
- ✅ 网络访问已允许（这个通常是默认允许的）

### 2.2 存储权限
- Android 13+: 需要 `照片和视频` 权限
- Android 6-12: 需要 `存储` 权限
- Android 5-: 自动授予

### 2.3 检查权限授予情况
在 Logcat 中查看：
```
FloatingWindow: 缺少读取媒体权限，无法监听系统截图
```

如果看到这条日志，说明权限未授予。

## 步骤3：手动测试网络请求

### 3.1 在电脑上测试服务器
```bash
curl -X POST http://shanghai.idc.matrixorigin.cn:30026/api/v1/recognize-heroes \
  -F "file=@/path/to/screenshot.png"
```

**期望响应：**
```json
{"heroes":["英雄1","英雄2","英雄3","英雄4","英雄5"],"success":true,"message":"成功识别 5 个英雄"}
```

### 3.2 检查手机网络
- 手机是否连接到互联网？
- 手机能否访问外网？
- 是否在公司/学校内网（可能有防火墙）？

## 步骤4：常见问题

### 问题1：看到 Toast "正在识别英雄..." 但没有后续
**原因：** 网络请求卡住或超时
**解决：** 查看 Logcat，应该能看到具体错误

### 问题2：看到 Toast "识别失败: xxx"
**原因：** 服务器返回错误或解析失败
**解决：** 查看具体错误信息

### 问题3：没有任何 Toast 提示
**原因1：** 截图没有被检测到
- 解决：检查存储权限
- 解决：确认截图保存在 `/DCIM/Screenshots/` 或类似目录

**原因2：** 回调没有被触发
- 解决：查看 Logcat 中 `ScreenshotObserver` 的日志

### 问题4：识别成功但英雄名字没更新
**原因：** UI 更新失败
**在 Logcat 中查找：**
```
FloatingWindow: 更新英雄名字 1: xxx
```

如果看到了这条日志，说明代码执行了，但 UI 可能没有刷新。

## 步骤5：清理缓存并重试

1. 停止 app
2. 在 `设置 > 应用 > 悬浮截屏` 中：
   - 清除缓存
   - 清除数据
3. 卸载并重新安装 app
4. 重新授予所有权限
5. 重新测试

## 步骤6：收集日志信息

如果以上步骤都无法解决，请收集以下信息：

1. **Logcat 完整日志**（从截图开始到等待2分钟后）
   - 过滤：`tag:FloatingWindow OR tag:ScreenshotObserver OR tag:HeroRecognitionService`

2. **手机信息**
   - Android 版本
   - 手机品牌和型号
   
3. **网络状态**
   - WiFi 还是移动网络
   - 能否访问外网

4. **截图信息**
   - 截图文件大小
   - 截图保存位置
   - 是否是游戏开局加载界面

## 快速诊断命令

### 在 Android Studio Terminal 中运行：

```bash
# 清除之前的日志
adb logcat -c

# 实时查看日志（过滤关键字）
adb logcat | grep -E "(FloatingWindow|ScreenshotObserver|HeroRecognitionService)"
```

然后进行截图操作，观察日志输出。

## 预期的完整日志流程

```
ScreenshotObserver: 检测到媒体库更新...
ScreenshotObserver: 已将截图复制到缓存: xxx
FloatingWindow: 检测到系统截图: xxx
FloatingWindow: ========== 开始上传截图进行识别 ==========
FloatingWindow: 截图路径: xxx
FloatingWindow: 文件是否存在: true
FloatingWindow: 文件大小: xxx bytes
HeroRecognitionService: 开始识别英雄，图片路径: xxx
HeroRecognitionService: 收到服务器响应，状态码: 200
HeroRecognitionService: 服务器响应内容: {"heroes":[...]}
FloatingWindow: 识别成功: [...]
FloatingWindow: 更新后的英雄列表: [...]
FloatingWindow: 已显示截图提示面板
FloatingWindow: 更新英雄名字 1: xxx
FloatingWindow: 更新英雄名字 2: xxx
...
```

如果你看到了上述完整日志，但英雄名字还是没有更新，那可能是 UI 层的问题。
