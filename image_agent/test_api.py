"""
测试 FastAPI 服务
"""
import requests
from pathlib import Path


def test_health_check():
    """测试健康检查接口"""
    print("测试健康检查...")
    response = requests.get("http://localhost:8000/")
    print(f"状态码: {response.status_code}")
    print(f"响应: {response.json()}")
    print()


def test_recognize_heroes(image_path: str):
    """测试英雄识别接口"""
    print(f"测试英雄识别（图片: {image_path}）...")
    
    image_file = Path(image_path)
    if not image_file.exists():
        print(f"错误: 图片文件不存在: {image_path}")
        return
    
    with open(image_path, 'rb') as f:
        files = {'file': f}
        response = requests.post(
            'http://localhost:8000/api/v1/recognize-heroes',
            files=files
        )
    
    print(f"状态码: {response.status_code}")
    if response.status_code == 200:
        result = response.json()
        print(f"识别成功: {result['success']}")
        print(f"消息: {result['message']}")
        print(f"英雄列表: {result['heroes']}")
    else:
        print(f"错误: {response.text}")
    print()


if __name__ == "__main__":
    # 测试健康检查
    test_health_check()
    
    # 测试英雄识别
    # 请替换为实际的图片路径
    image_path = "agent/game_start.jpg"
    test_recognize_heroes(image_path)

