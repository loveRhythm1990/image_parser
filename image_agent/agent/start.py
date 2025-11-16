"""
FastAPI 服务：王者荣耀英雄识别
接收图片，返回识别出的5个敌方英雄列表
参考文档：https://docs.siliconflow.cn/cn/api-reference/chat-completions/chat-completions#vlm
"""

from __future__ import annotations

import base64
import os
from pathlib import Path
from typing import Any, Dict, Optional
import json
import requests

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel

# 创建 FastAPI 应用
app = FastAPI(
    title="王者荣耀英雄识别服务",
    description="识别游戏开局加载图中的敌方英雄",
    version="1.0.0"
)

API_URL = "https://api.siliconflow.cn/v1/chat/completions"
MODEL_NAME = "Qwen/Qwen3-VL-32B-Instruct"


class HeroResponse(BaseModel):
    """英雄识别响应"""
    heroes: list[str]
    success: bool
    message: str = ""

def encode_image_to_base64(image_path: Path) -> str:
    """将图片编码为 base64，并返回 data URL"""
    data = image_path.read_bytes()
    b64 = base64.b64encode(data).decode("utf-8")
    suffix = image_path.suffix.lower()
    if suffix in {".jpg", ".jpeg"}:
        mime = "image/jpeg"
    elif suffix == ".png":
        mime = "image/png"
    else:
        # 默认按 png 处理
        mime = "image/png"
    return f"data:{mime};base64,{b64}"


def encode_image_bytes_to_base64(image_bytes: bytes, mime_type: str = "image/jpeg") -> str:
    """将图片字节编码为 base64 data URL"""
    b64 = base64.b64encode(image_bytes).decode("utf-8")
    return f"data:{mime_type};base64,{b64}"


def build_payload(
    prompt: str, 
    image_data_url: Optional[str] = None, 
    json_output: bool = True,
) -> Dict[str, Any]:
    
    """构建请求体，使用 OpenAI 兼容的格式"""
    content: list[Dict[str, Any]] = []
    if image_data_url:
        content.append({
            "type": "image_url",
            "image_url": {
                "url": image_data_url,
                "detail": "high"
            }
        })
    
    content.append({
        "type": "text",
        "text": prompt
    })

    payload = {
        "model": MODEL_NAME,
        "messages": [
            {
                "role": "user",
                "content": content,
            }
        ],
        "temperature": 0.5,
        "max_tokens": 4096,
    }
    
    # 如果需要 JSON 格式输出
    if json_output:
        payload["response_format"] = {"type": "json_object"}
    
    return payload


def analyze(prompt: str, image_path: Optional[str] = None, json_output: bool = False) -> Dict[str, Any]:
    """
    调用 SiliconFlow 接口识别图片文字或进行图像分析。
    需要提前在环境变量中设置 SILICONFLOW_API_KEY。
    
    Args:
        prompt: 提示词
        image_path: 图片路径（可选）
        json_output: 是否要求模型返回 JSON 格式（需要在 prompt 中说明 JSON 结构）
    """
    token = os.getenv("SILICONFLOW_API_KEY", "sk-skluaknphpprbznvqaqswtampdrweaxhwmdtjlvinjguxmsk")
    image_data_url: Optional[str] = None

    if image_path:
        image_file = Path(image_path)
        if not image_file.exists():
            raise FileNotFoundError(f"未找到图片文件: {image_path}")
        image_data_url = encode_image_to_base64(image_file)

    payload = build_payload(prompt, image_data_url, json_output)
    
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }

    response = requests.post(API_URL, headers=headers, json=payload, timeout=600)
    
    if response.status_code != 200:
        response.raise_for_status()
    
    return response.json()


def analyze_heroes_from_bytes(image_bytes: bytes, mime_type: str = "image/jpeg") -> list[str]:
    """
    从图片字节数据中分析英雄
    
    Args:
        image_bytes: 图片字节数据
        mime_type: 图片 MIME 类型
    
    Returns:
        英雄名字列表
    """
    token = os.getenv("SILICONFLOW_API_KEY", "sk-skluaknphpprbznvqaqswtampdrweaxhwmdtjlvinjguxmsk")
    
    image_data_url = encode_image_bytes_to_base64(image_bytes, mime_type)
    
    prompt = """
    这是一张王者荣耀开局加载图，请识别图中的 5 个**敌方**英雄
    ## 注意：
    1. 识别**敌方**五个英雄，
       每个英雄中间有两行字，第一行是英雄名字，第二行为玩家名字
       **如果玩家名字有淡黄色背景，说明这一排英雄均为我方英雄**
       我需要识别敌方英雄
    2. 识别顺序从左到右顺序
    3. 输出英雄名字

    ## 输出格式（JSON）：
    {
        "heroes": ["英雄1", "英雄2", "英雄3", "英雄4", "英雄5"]
    }
    """
    
    payload = build_payload(prompt, image_data_url, json_output=True)
    
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }

    response = requests.post(API_URL, headers=headers, json=payload, timeout=600)
    
    if response.status_code != 200:
        raise HTTPException(
            status_code=response.status_code,
            detail=f"VLM API 调用失败: {response.text}"
        )
    
    result = response.json()
    
    # 解析模型返回的内容
    choices = result.get("choices", [])
    if not choices:
        raise HTTPException(status_code=500, detail="未获取到模型回复")
    
    message = choices[0].get("message", {})
    content = message.get("content")
    
    if not content:
        raise HTTPException(status_code=500, detail="模型回复内容为空")
    
    try:
        data = json.loads(content)
        heroes = data.get("heroes", [])
        
        # 清理英雄名字（去除可能的编号前缀）
        cleaned_heroes = []
        for h in heroes:
            parts = h.split(' ', 1) 
            if len(parts) > 1:
                cleaned_heroes.append(parts[1])
            else:
                cleaned_heroes.append(h)
        
        return cleaned_heroes
    except json.JSONDecodeError as e:
        raise HTTPException(
            status_code=500,
            detail=f"解析模型返回的 JSON 失败: {str(e)}, 原始内容: {content}"
        )


@app.get("/")
async def root():
    """健康检查接口"""
    return {"status": "ok", "service": "王者荣耀英雄识别服务"}


@app.get("/health")
async def health_check():
    """健康检查接口"""
    return {"status": "healthy", "service": "hero-recognition-api"}


@app.post("/api/v1/recognize-heroes", response_model=HeroResponse)
async def recognize_heroes(file: UploadFile = File(...)):
    """
    识别王者荣耀开局加载图中的敌方英雄
    
    Args:
        file: 上传的图片文件（支持 jpg, jpeg, png）
    
    Returns:
        HeroResponse: 包含英雄列表的响应
    """
    # 验证文件类型
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(
            status_code=400,
            detail=f"不支持的文件类型: {file.content_type}，请上传图片文件"
        )
    
    try:
        # 读取图片数据
        image_bytes = await file.read()
        
        if len(image_bytes) == 0:
            raise HTTPException(status_code=400, detail="上传的图片文件为空")
        
        # 调用识别服务
        heroes = analyze_heroes_from_bytes(image_bytes, file.content_type)
        
        return HeroResponse(
            heroes=heroes,
            success=True,
            message=f"成功识别 {len(heroes)} 个英雄"
        )
    
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"识别失败: {str(e)}"
        )


def main() -> None:
    """
    命令行测试调用示例：
    1. export SILICONFLOW_API_KEY=xxx
    2. python -m image_agent.agent.start
    """

    prompt = """
    这是一张王者荣耀开局加载图，请识别图中的 5 个**敌方**英雄
    ## 注意：
    1. 识别**敌方**五个英雄，
       每个英雄中间有两行字，第一行是英雄名字，第二行为玩家名字
       **如果玩家名字有淡黄色背景，说明这一排英雄均为我方英雄**
       我需要识别敌方英雄
    2. 识别顺序从左到右顺序
    3. 输出英雄名字

    ## 输出格式（JSON）：
    {
        "heroes": ["英雄 1", "英雄 2", "英雄 3", "英雄 4", "英雄 5"]
    }
    """
    image_path: Optional[str] = "game_start.jpg"

    try:
        result = analyze(prompt, image_path, json_output=True)
    except Exception as exc:
        print(f"请求失败: {exc}")
        return

    # 打印模型返回的第一条消息
    choices = result.get("choices", [])
    if not choices:
        print("未获取到模型回复")
        return

    message = choices[0].get("message", {})
    content = message.get("content")
    
    try:
        data = json.loads(content)
        heros = data.get("heroes")
        rst: list = []
        for h in heros:
            parts = h.split(' ', 1) 
            if len(parts) > 1:
                rst.append(parts[1])
            else:
                rst.append(h)
        print(rst)
    except:
        pass


if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == "serve":
        # 启动 FastAPI 服务
        import uvicorn
        uvicorn.run(app, host="0.0.0.0", port=8000)
    else:
        # 运行命令行测试
        main()