# 英雄识别功能说明

## 功能概述

该功能通过系统截图自动识别《王者荣耀》游戏开局加载界面中的敌方英雄，并自动配置技能 CD 倒计时。

## 主要特性

### 1. 自动截图检测
- 监听系统截图目录 `/DCIM/Screenshots/`
- 检测到新截图时自动上传到服务器进行识别
- 支持 Android 9 及以下版本直接读取文件路径
- 支持 Android 10+ 通过 MediaStore API 复制截图到缓存

### 2. 英雄识别
- 上传截图到 FastAPI 服务器：`http://shanghai.idc.matrixorigin.cn:30026/api/v1/recognize-heroes`
- 服务器返回识别出的5位英雄名字
- 自动在 `hero_spec.json` 中查找匹配的英雄数据
- 未识别或未找到的英雄显示为"英雄1"至"英雄5"

### 3. 智能 CD 管理
- **大招 CD**：从 `hero_spec.json` 的 `skill4_cooldown` 字段读取
- **技能 CD**：从 `common_skill.json` 读取（闪现、惩戒等11种技能）
- 默认倒计时为 0，不自动启动
- 点击技能按钮时根据英雄/技能动态获取 CD 时间并启动倒计时

### 4. 用户交互
- **大招按钮**：点击启动对应英雄的大招倒计时
- **技能按钮**：
  - 点击：启动当前选中技能的倒计时
  - 长按：显示技能选择面板（可滚动，包含11种技能）

## 技术实现

### 新增文件

1. **HeroRecognitionService.kt**
   - 封装 OkHttp 网络请求
   - 调用 FastAPI 服务进行英雄识别
   - 不设置超时时间，适应服务器处理时间较长的情况

2. **HeroDataManager.kt**
   - 加载并解析 `hero_spec.json` 和 `common_skill.json`
   - 提供英雄大招 CD 和通用技能 CD 的查询接口
   - 支持模糊查找和默认值

3. **Assets 文件**
   - `app/src/main/assets/hero_spec.json`：包含所有英雄的大招数据
   - `app/src/main/assets/common_skill.json`：包含11种通用技能的 CD 数据

### 修改文件

1. **AndroidManifest.xml**
   - 添加 `INTERNET` 权限
   - 添加 `ACCESS_NETWORK_STATE` 权限

2. **app/build.gradle**
   - 添加 OkHttp 依赖：`implementation 'com.squareup.okhttp3:okhttp:4.12.0'`

3. **FloatingWindowService.kt**
   - 集成 `HeroDataManager` 和 `HeroRecognitionService`
   - 添加 `uploadScreenshotForRecognition()` 方法上传截图
   - 添加 `updateHeroes()` 方法更新英雄列表
   - 添加 `updateHeroNamesDisplay()` 方法更新 UI 显示
   - 添加 `initializeCountdownsToZero()` 方法初始化倒计时为 0
   - 添加 `startUltimateCountdown()` 方法处理大招倒计时
   - 添加 `startSkillCountdown()` 方法处理技能倒计时
   - 添加 `startCountdownWithDuration()` 方法统一管理倒计时

4. **ScreenshotObserver.kt**
   - 修改回调参数从文件名改为完整路径
   - 添加 `getFullPathFromResult()` 获取文件完整路径
   - 添加 `getRealPathFromUri()` 处理 Android 10+ Uri 转换
   - 添加 `copyUriToCache()` 将截图复制到缓存目录（Android 10+）

## 数据格式

### hero_spec.json
```json
[
  {
    "name": "上官婉儿",
    "title": "惊鸿之笔",
    "skill4_name": "章草·横鳞",
    "skill4_cooldown": "40",
    "skill4_cost": "100",
    "skill4_description": "..."
  }
]
```

### common_skill.json
```json
{
  "闪现": 120,
  "惩戒": 30,
  "终结": 60,
  "狂暴": 75,
  "疾跑": 75,
  "治疗": 120,
  "干扰": 90,
  "晕眩": 90,
  "净化": 120,
  "弱化": 75,
  "传送": 75
}
```

## 使用流程

1. 用户启动悬浮窗服务
2. 使用系统截图功能截取游戏开局加载界面
3. App 自动检测到截图并上传到服务器
4. 服务器返回识别出的5位英雄名字
5. App 自动更新悬浮窗面板，显示英雄名字
6. 所有倒计时初始化为 0
7. 用户点击"大招"或"技能"按钮启动对应的 CD 倒计时
8. 用户长按"技能"按钮可以更换技能

## 容错处理

- **网络请求失败**：显示错误提示，使用默认英雄名字（英雄1-5）
- **英雄未找到**：显示默认名字（英雄1-5）
- **CD 为 0**：不启动倒计时，在日志中记录警告
- **Android 版本兼容**：自动适配 Android 9- 和 Android 10+ 的文件访问方式

## 注意事项

1. 首次使用需要授予网络访问权限和存储访问权限
2. 服务器处理时间较长，请耐心等待（会显示"正在识别英雄..."提示）
3. 建议在稳定的网络环境下使用
4. 截图质量会影响识别准确率，建议使用清晰的游戏开局加载界面截图
5. 如果识别失败，可以手动点击技能按钮使用默认配置

