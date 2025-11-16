# 王者荣耀英雄识别 API 服务

基于 FastAPI 的王者荣耀开局加载图英雄识别服务。

## 功能特性

- 接收游戏开局加载图，识别敌方 5 个英雄
- RESTful API 接口
- Docker 容器化部署
- 健康检查和日志管理

## 快速开始

### 1. 使用 Docker Compose（推荐）

```bash
# 克隆项目后进入目录
cd image_agent

# （可选）配置 API Key
# 复制环境变量示例文件
cp env.example .env
# 编辑 .env 文件，填入你的 SiliconFlow API Key

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

### 2. 本地开发

```bash
# 安装依赖
pip install -r requirements.txt

# 设置环境变量（可选）
export SILICONFLOW_API_KEY=your_api_key_here

# 启动服务（方式一）
python -m uvicorn agent.start:app --host 0.0.0.0 --port 8000 --reload

# 启动服务（方式二）
cd agent
python start.py serve
```

## API 使用

### 健康检查

```bash
curl http://localhost:8000/
```

响应：
```json
{
  "status": "ok",
  "service": "王者荣耀英雄识别服务"
}
```

### 识别英雄

```bash
curl -X POST http://localhost:8000/api/v1/recognize-heroes \
  -F "file=@game_start.jpg"
```

响应示例：
```json
{
  "heroes": ["孙悟空", "妲己", "亚瑟", "鲁班七号", "安琪拉"],
  "success": true,
  "message": "成功识别 5 个英雄"
}
```

### 使用 Python 调用

```python
import requests

# 上传图片
with open('game_start.jpg', 'rb') as f:
    files = {'file': f}
    response = requests.post(
        'http://localhost:8000/api/v1/recognize-heroes',
        files=files
    )
    
result = response.json()
print(f"识别结果: {result['heroes']}")
```

### 使用 curl 调用

```bash
# 识别本地图片
curl -X POST "http://localhost:8000/api/v1/recognize-heroes" \
  -H "accept: application/json" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/your/game_start.jpg"
```

## API 文档

启动服务后，访问以下地址查看交互式 API 文档：

- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SILICONFLOW_API_KEY` | SiliconFlow API 密钥 | 代码中硬编码的默认值 |

### 端口配置

默认端口为 `8000`，可以在 `docker-compose.yaml` 中修改：

```yaml
ports:
  - "9000:8000"  # 将宿主机的 9000 端口映射到容器的 8000 端口
```

## Docker 命令

```bash
# 构建镜像
docker build -t hero-recognition-api .

# 运行容器
docker run -d \
  --name hero-api \
  -p 8000:8000 \
  -e SILICONFLOW_API_KEY=your_key \
  hero-recognition-api

# 查看日志
docker logs -f hero-api

# 停止容器
docker stop hero-api

# 删除容器
docker rm hero-api
```

## 项目结构

```
image_agent/
├── agent/
│   ├── start.py             # FastAPI 应用主文件（整合了识别逻辑）
│   └── __init__.py          # Python 包标识
├── Dockerfile               # Docker 镜像构建文件
├── docker-compose.yaml      # Docker Compose 配置
├── requirements.txt         # Python 依赖
├── .dockerignore           # Docker 忽略文件
├── env.example             # 环境变量示例
└── README_API.md           # API 文档（本文件）
```

## 注意事项

1. **API Key 安全**：建议使用环境变量或 `.env` 文件管理 API Key，不要硬编码在代码中
2. **图片格式**：支持 JPG、JPEG、PNG 格式
3. **超时设置**：VLM 模型调用超时时间为 600 秒
4. **并发处理**：默认配置适合中小规模请求，高并发场景请调整 uvicorn 的 worker 数量

## 性能优化

如需提高并发处理能力，可以修改启动命令：

```bash
# 使用多个 worker
uvicorn agent.start:app --host 0.0.0.0 --port 8000 --workers 4
```

或在 `docker-compose.yaml` 中修改：

```yaml
command: ["uvicorn", "agent.start:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
```

## 故障排查

### 1. 容器无法启动

```bash
# 查看详细日志
docker-compose logs hero-recognition-api

# 检查端口是否被占用
lsof -i :8000
```

### 2. API 调用失败

检查：
- SiliconFlow API Key 是否正确
- 网络是否可以访问 api.siliconflow.cn
- 上传的图片格式是否正确

### 3. 识别结果不准确

- 确保上传的是王者荣耀开局加载图
- 图片清晰度要足够
- 敌方英雄区域要完整

## 许可证

MIT License

