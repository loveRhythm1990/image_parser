# 快速启动指南

## 🚀 Docker 方式（推荐）

### 1. 启动服务

```bash
cd image_agent
docker-compose up -d
```

### 2. 查看日志

```bash
docker-compose logs -f
```

### 3. 测试服务

```bash
# 健康检查
curl http://localhost:8000/

# 识别英雄
curl -X POST http://localhost:8000/api/v1/recognize-heroes \
  -F "file=@agent/game_start.jpg"
```

### 4. 停止服务

```bash
docker-compose down
```

---

## 💻 本地开发方式

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 启动服务

```bash
# 方式一：使用 uvicorn 命令
python -m uvicorn agent.start:app --host 0.0.0.0 --port 8000 --reload

# 方式二：直接运行脚本
cd agent
python start.py serve
```

### 3. 测试服务

```bash
# 在另一个终端运行
python test_api.py
```

---

## 📖 API 文档

启动服务后访问：

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

---

## 🔧 配置 API Key（可选）

如果需要使用自己的 SiliconFlow API Key：

```bash
# 创建 .env 文件
cp env.example .env

# 编辑 .env 文件，填入你的 API Key
# SILICONFLOW_API_KEY=your_api_key_here
```

---

## 📝 使用示例

### Python 调用

```python
import requests

# 上传图片识别英雄
with open('game_start.jpg', 'rb') as f:
    files = {'file': f}
    response = requests.post(
        'http://localhost:8000/api/v1/recognize-heroes',
        files=files
    )
    
result = response.json()
print(f"识别的英雄: {result['heroes']}")
```

### curl 调用

```bash
curl -X POST "http://localhost:8000/api/v1/recognize-heroes" \
  -H "accept: application/json" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/your/image.jpg"
```

### 响应示例

```json
{
  "heroes": ["孙悟空", "妲己", "亚瑟", "鲁班七号", "安琪拉"],
  "success": true,
  "message": "成功识别 5 个英雄"
}
```

---

## ❓ 常见问题

### 端口已被占用

修改 `docker-compose.yaml` 中的端口映射：

```yaml
ports:
  - "9000:8000"  # 将宿主机端口改为 9000
```

### 容器启动失败

```bash
# 查看详细日志
docker-compose logs hero-recognition-api

# 重新构建镜像
docker-compose build --no-cache
docker-compose up -d
```

### 识别失败

- 检查上传的图片是否为王者荣耀开局加载图
- 确保图片清晰，敌方英雄区域完整
- 查看服务日志了解详细错误信息

