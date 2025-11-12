# 王者荣耀英雄信息爬虫

这是一个用于爬取王者荣耀官网英雄信息的爬虫工具。

## 功能特性

- 爬取英雄详情页面内容
- 爬取英雄列表页面内容
- 自动保存为文本文件
- 支持中文编码处理

## 安装依赖

本项目使用 Poetry 管理依赖。

### 使用 Poetry（推荐）

```bash
# 安装依赖
poetry install --no-root

# 运行爬虫
poetry run python scraper.py
```

### 使用传统方式

如果你不想使用 Poetry，也可以使用传统的 pip：

```bash
pip install requests beautifulsoup4
python scraper.py
```

## 使用方法

### 基本使用（爬取指定页面）

```bash
# 使用 Poetry
poetry run python scraper.py

# 或直接使用 Python（需要先安装依赖）
python scraper.py
```

### 批量爬取所有英雄详情

```bash
# 使用 Poetry
poetry run python scraper.py --all-heroes

# 或直接使用 Python
python scraper.py --all-heroes
```

这会自动从英雄列表页面提取所有英雄链接，然后批量爬取每个英雄的详情页面。

**注意**：批量爬取可能需要较长时间（约100+个英雄，每个间隔2秒），请耐心等待。

### 自定义爬取

修改 `scraper.py` 中的 `main()` 函数，添加或修改要爬取的 URL：

```python
urls = [
    {
        'url': 'https://pvp.qq.com/web201605/herodetail/chicha.shtml',
        'type': 'detail'
    },
    {
        'url': 'https://pvp.qq.com/web201605/herolist.shtml',
        'type': 'list'
    }
]
```

## 输出目录

爬取的内容会保存在 `scraped_content/` 目录下，文件名格式为：
- `{页面名称}_{类型}_{时间戳}.txt`

例如：
- `chicha.shtml_detail_20240101_120000.txt`
- `herolist.shtml_list_20240101_120000.txt`

## 注意事项

1. 请遵守网站的 robots.txt 和使用条款
2. 爬取时会有 2 秒延迟，避免请求过快
3. 如果遇到编码问题，脚本会自动尝试多种编码方式
4. 建议合理使用，不要频繁请求

## 项目结构

```
web_scraper/
├── scraper.py              # 主爬虫脚本
├── requirements.txt        # Python依赖
├── README.md              # 说明文档
└── scraped_content/       # 爬取内容保存目录
    └── *.txt              # 保存的文本文件
```

