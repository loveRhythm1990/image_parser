#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
王者荣耀英雄信息爬虫
爬取指定页面的内容并保存到文本文件
"""

import os
import re
import time
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse
from datetime import datetime
import chardet


class HeroScraper:
    def __init__(self, output_dir="scraped_content"):
        """
        初始化爬虫
        
        Args:
            output_dir: 输出目录，用于保存爬取的内容
        """
        self.base_url = "https://pvp.qq.com"
        self.session = requests.Session()
        # 设置请求头，模拟浏览器访问
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
            'Accept-Encoding': 'gzip, deflate, br',
            'Connection': 'keep-alive',
            'Upgrade-Insecure-Requests': '1',
            'Referer': 'https://pvp.qq.com/',
        })
        
        # 确保输出目录存在
        script_dir = os.path.dirname(os.path.abspath(__file__))
        self.output_dir = os.path.join(script_dir, output_dir)
        os.makedirs(self.output_dir, exist_ok=True)
        
    def detect_encoding(self, content):
        """
        检测内容编码
        
        Args:
            content: 字节内容
            
        Returns:
            检测到的编码
        """
        try:
            result = chardet.detect(content)
            encoding = result.get('encoding', 'utf-8')
            confidence = result.get('confidence', 0)
            
            # 如果置信度较低，优先尝试常见的中文编码
            if confidence < 0.7:
                # 尝试 GBK/GB2312
                try:
                    content.decode('gbk')
                    return 'gbk'
                except:
                    pass
                try:
                    content.decode('gb2312')
                    return 'gb2312'
                except:
                    pass
            
            return encoding or 'utf-8'
        except:
            return 'utf-8'
    
    def clean_text(self, text):
        """
        清理文本内容，移除多余的空白字符和特殊字符
        
        Args:
            text: 原始文本
            
        Returns:
            清理后的文本
        """
        if not text:
            return ""
        
        # 移除控制字符（保留换行符和制表符）
        text = re.sub(r'[\x00-\x08\x0b-\x0c\x0e-\x1f\x7f-\x9f]', '', text)
        
        # 移除多余的空白字符（保留单个空格）
        text = re.sub(r'[ \t]+', ' ', text)
        
        # 移除多余的换行符（保留最多两个连续换行）
        text = re.sub(r'\n{3,}', '\n\n', text)
        
        # 移除首尾空白
        text = text.strip()
        
        return text
    
    def extract_text_from_element(self, element, min_length=3):
        """
        从元素中提取文本，过滤掉无用的内容
        
        Args:
            element: BeautifulSoup 元素
            min_length: 最小文本长度
            
        Returns:
            提取的文本列表
        """
        texts = []
        
        # 移除 script 和 style 标签
        for tag in element.find_all(['script', 'style', 'noscript']):
            tag.decompose()
        
        # 提取所有文本节点
        for text_node in element.stripped_strings:
            cleaned = self.clean_text(text_node)
            if cleaned and len(cleaned) >= min_length:
                # 过滤掉纯数字、纯符号等无意义内容
                if not re.match(r'^[\d\s\-_\.]+$', cleaned):
                    texts.append(cleaned)
        
        return texts
    
    def get_page_content(self, url):
        """
        获取网页内容，自动检测编码
        
        Args:
            url: 目标URL
            
        Returns:
            网页内容（BeautifulSoup对象）和原始响应
        """
        try:
            print(f"正在访问: {url}")
            response = self.session.get(url, timeout=30, allow_redirects=True)
            response.raise_for_status()
            
            # 检测编码
            detected_encoding = self.detect_encoding(response.content)
            
            # 尝试多种编码方式
            encodings_to_try = [detected_encoding, 'gbk', 'gb2312', 'utf-8', 'big5']
            content = None
            
            for enc in encodings_to_try:
                try:
                    content = response.content.decode(enc)
                    print(f"  使用编码: {enc}")
                    break
                except (UnicodeDecodeError, LookupError):
                    continue
            
            if content is None:
                # 如果所有编码都失败，使用 errors='ignore'
                content = response.content.decode('utf-8', errors='ignore')
                print(f"  警告: 使用 UTF-8 并忽略错误")
            
            # 使用 html.parser 解析
            soup = BeautifulSoup(content, 'html.parser')
            return soup, response
            
        except requests.exceptions.RequestException as e:
            print(f"  ✗ 请求失败: {e}")
            return None, None
        except Exception as e:
            print(f"  ✗ 解析失败: {e}")
            return None, None
    
    def extract_hero_name(self, soup):
        """
        从页面中提取英雄名字
        
        Args:
            soup: BeautifulSoup对象
            
        Returns:
            英雄名字，如果找不到则返回None
        """
        # 优先从h2标签提取（通常是英雄名字）
        h2_tags = soup.find_all('h2')
        for h2 in h2_tags:
            text = self.clean_text(h2.get_text())
            if text and len(text) <= 10:  # 英雄名字通常不会太长
                # 过滤掉明显不是名字的内容
                if not any(keyword in text for keyword in ['技能', '介绍', '建议', '关系', '攻略', '出装', '铭文']):
                    return text
        
        # 从h1标签提取
        h1_tags = soup.find_all('h1')
        for h1 in h1_tags:
            text = self.clean_text(h1.get_text())
            if text and len(text) <= 10:
                if not any(keyword in text for keyword in ['技能', '介绍', '建议', '关系', '攻略', '出装', '铭文']):
                    return text
        
        # 从页面标题中提取（格式通常是：王者荣耀XXX-...）
        title = soup.find('title')
        if title:
            title_text = self.clean_text(title.get_text())
            # 尝试从"王者荣耀XXX"中提取
            match = re.search(r'王者荣耀([^-]+)', title_text)
            if match:
                hero_name = match.group(1).strip()
                if hero_name and len(hero_name) <= 10:
                    return hero_name
        
        return None
    
    def extract_skills(self, soup):
        """
        提取技能信息，返回格式化的技能列表
        
        Args:
            soup: BeautifulSoup对象
            
        Returns:
            技能列表，每个技能是 (技能名, 冷却值, 消耗, 描述) 的元组
        """
        skills = []
        
        # 获取所有文本，按行处理
        all_text = soup.get_text()
        lines = [line.strip() for line in all_text.split('\n') if line.strip()]
        
        current_skill = None
        current_cd = None
        current_cost = None
        current_desc = []
        in_skill_section = False
        
        # 技能名的常见模式
        skill_name_patterns = [
            r'^[^：:]+$',  # 不包含冒号的短文本
        ]
        
        for i, line in enumerate(lines):
            # 检测是否进入技能介绍部分
            if '技能介绍' in line or ('冷却值' in line and '消耗' in line):
                in_skill_section = True
            
            # 如果不在技能部分，跳过
            if not in_skill_section:
                continue
            
            # 如果遇到其他章节标题，停止
            if any(kw in line for kw in ['铭文', '出装', '英雄关系', '技能加点', '英雄攻略']):
                if current_skill and current_desc:
                    skills.append((current_skill, current_cd, current_cost, '\n'.join(current_desc)))
                break
            
            # 检测冷却值
            if '冷却值' in line:
                cd_match = re.search(r'冷却值[：:]\s*([^\n]+)', line)
                if cd_match:
                    current_cd = cd_match.group(1).strip()
                continue
            
            # 检测消耗
            if '消耗' in line and '冷却值' not in line:
                cost_match = re.search(r'消耗[：:]\s*([^\n]+)', line)
                if cost_match:
                    current_cost = cost_match.group(1).strip()
                continue
            
            # 检测技能名（通常是短文本，且不包含关键词）
            if (len(line) <= 20 and 
                not any(kw in line for kw in ['冷却值', '消耗', '技能', '介绍', '建议', '关系', '攻略', '出装', '铭文', 'Tips', '主升', '副升', '召唤师']) and
                '：' not in line and ':' not in line):
                
                # 保存上一个技能
                if current_skill and current_desc:
                    skills.append((current_skill, current_cd, current_cost, '\n'.join(current_desc)))
                
                # 开始新技能
                current_skill = line
                current_cd = None
                current_cost = None
                current_desc = []
            
            # 技能描述（较长的文本，且不是技能名）
            elif current_skill and len(line) > 15:
                # 过滤掉明显不是描述的内容
                if not any(kw in line for kw in ['技能介绍', '铭文', '出装', '英雄关系']):
                    current_desc.append(line)
        
        # 保存最后一个技能
        if current_skill and current_desc:
            skills.append((current_skill, current_cd, current_cost, '\n'.join(current_desc)))
        
        return skills
    
    def scrape_hero_detail(self, url):
        """
        爬取英雄详情页面，优化排版
        
        Args:
            url: 英雄详情页URL
            
        Returns:
            (英雄名字, 提取的文本内容) 元组，如果失败返回 (None, None)
        """
        soup, response = self.get_page_content(url)
        if not soup:
            return None, None
        
        # 提取英雄名字
        hero_name = self.extract_hero_name(soup)
        
        content_parts = []
        seen_texts = set()  # 用于去重
        
        # 英雄基本信息
        content_parts.append("=" * 80)
        if hero_name:
            content_parts.append(f"英雄名称：{hero_name}")
        content_parts.append("=" * 80)
        content_parts.append("")
        
        # 提取英雄称号（通常是h3中的第一个，且不是"技能介绍"等）
        h3_tags = soup.find_all('h3')
        hero_title = None
        for h3 in h3_tags:
            text = self.clean_text(h3.get_text())
            if text and len(text) <= 15 and not any(kw in text for kw in ['技能', '介绍', '建议', '关系', '攻略', '出装', '铭文']):
                hero_title = text
                break
        
        if hero_title:
            content_parts.append(f"【英雄称号】{hero_title}\n")
        
        # 提取技能信息
        content_parts.append("【技能介绍】")
        content_parts.append("-" * 80)
        
        # 从原始文本中提取技能信息
        # 格式通常是：技能名冷却值：XX消耗：XX
        all_text = soup.get_text()
        lines = [line.strip() for line in all_text.split('\n') if line.strip()]
        
        skill_items = []
        in_skill_section = False
        
        for i, line in enumerate(lines):
            # 检测是否进入技能介绍部分
            if '技能介绍' in line:
                in_skill_section = True
                continue
            
            # 如果不在技能部分，跳过
            if not in_skill_section:
                continue
            
            # 如果遇到其他章节，停止
            if any(kw in line for kw in ['铭文', '出装', '英雄关系', '技能加点', '英雄攻略']):
                break
            
            # 检测技能行：包含"冷却值"和"消耗"，且在同一行
            if '冷却值' in line and '消耗' in line:
                # 提取技能名、冷却值、消耗
                # 格式：技能名冷却值：XX消耗：XX
                skill_match = re.match(r'^([^冷]+)冷却值[：:]\s*([^消]+)消耗[：:]\s*(.+)$', line)
                if skill_match:
                    skill_name = skill_match.group(1).strip()
                    cd = skill_match.group(2).strip()
                    cost = skill_match.group(3).strip()
                    
                    # 获取下一行作为技能描述
                    desc = ""
                    if i + 1 < len(lines):
                        next_line = lines[i + 1]
                        # 如果下一行是描述（较长且不包含冷却值/消耗）
                        if len(next_line) > 20 and '冷却值' not in next_line and '消耗' not in next_line:
                            desc = next_line
                            # 继续查找更多描述行
                            j = i + 2
                            while j < len(lines) and len(lines[j]) > 20 and '冷却值' not in lines[j] and '消耗' not in lines[j]:
                                if any(kw in lines[j] for kw in ['铭文', '出装', '英雄关系', '技能加点']):
                                    break
                                desc += " " + lines[j]
                                j += 1
                    
                    skill_items.append((skill_name, cd, cost, desc))
        
        # 输出技能信息
        if skill_items:
            for idx, (skill_name, cd, cost, desc) in enumerate(skill_items, 1):
                content_parts.append(f"\n技能 {idx}：{skill_name}")
                content_parts.append(f"  冷却值：{cd}")
                content_parts.append(f"  消耗：{cost}")
                if desc:
                    # 格式化描述，适当换行
                    desc_cleaned = self.clean_text(desc)
                    # 如果描述很长，适当分段
                    if len(desc_cleaned) > 80:
                        # 尝试在句号、逗号处换行
                        desc_parts = re.split(r'([。，；])', desc_cleaned)
                        formatted_desc = ""
                        current_line = ""
                        for part in desc_parts:
                            if len(current_line + part) > 60 and current_line:
                                formatted_desc += current_line + "\n"
                                current_line = part
                            else:
                                current_line += part
                        if current_line:
                            formatted_desc += current_line
                        content_parts.append(f"  描述：{formatted_desc}")
                    else:
                        content_parts.append(f"  描述：{desc_cleaned}")
                content_parts.append("")
        else:
            content_parts.append("\n（未能提取到结构化技能信息）\n")
        
        content_parts.append("-" * 80)
        content_parts.append("")
        
        # 提取其他重要信息（铭文、出装等）
        h3_sections = {}
        for h3 in h3_tags:
            text = self.clean_text(h3.get_text())
            if text and text not in seen_texts:
                h3_sections[text] = []
                seen_texts.add(text)
        
        # 提取每个h3后面的内容
        for h3 in h3_tags:
            section_title = self.clean_text(h3.get_text())
            if not section_title or section_title in ['技能介绍']:
                continue
            
            # 获取h3后面的兄弟元素
            next_elements = []
            current = h3.find_next_sibling()
            count = 0
            while current and count < 10:
                if current.name in ['h2', 'h3']:
                    break
                text = self.clean_text(current.get_text())
                if text and len(text) > 5:
                    next_elements.append(text)
                current = current.find_next_sibling()
                count += 1
            
            if next_elements:
                content_parts.append(f"\n【{section_title}】")
                content_parts.append("-" * 80)
                for elem in next_elements[:15]:  # 限制数量
                    if elem not in seen_texts:
                        content_parts.append(f"  {elem}")
                        seen_texts.add(elem)
                content_parts.append("")
        
        # 提取英雄故事/背景
        story_keywords = ['故事', '背景', '历史', '传记']
        paragraphs = soup.find_all('p')
        story_texts = []
        for p in paragraphs:
            text = self.clean_text(p.get_text())
            if text and len(text) > 50:  # 故事通常较长
                # 检查是否包含故事相关内容
                if any(kw in text for kw in story_keywords) or len(text) > 100:
                    if text not in seen_texts:
                        story_texts.append(text)
                        seen_texts.add(text)
        
        if story_texts:
            content_parts.append("\n【英雄故事】")
            content_parts.append("-" * 80)
            for story in story_texts[:5]:  # 限制数量
                content_parts.append(f"{story}\n")
            content_parts.append("")
        
        return hero_name, "\n".join(content_parts)
    
    def extract_hero_urls_from_list(self, url):
        """
        从英雄列表页面提取所有英雄详情页URL
        
        Args:
            url: 英雄列表页URL
            
        Returns:
            英雄URL列表，格式: [(英雄名, URL), ...]
        """
        soup, response = self.get_page_content(url)
        if not soup:
            return []
        
        hero_urls = []
        links = soup.find_all('a', href=True)
        
        for link in links:
            text = self.clean_text(link.get_text())
            href = link.get('href', '')
            if text and 'herodetail' in href:
                full_url = urljoin(url, href)
                hero_urls.append((text, full_url))
        
        # 去重（基于URL）
        seen_urls = set()
        unique_hero_urls = []
        for name, url in hero_urls:
            if url not in seen_urls:
                seen_urls.add(url)
                unique_hero_urls.append((name, url))
        
        return unique_hero_urls
    
    def scrape_hero_list(self, url):
        """
        爬取英雄列表页面
        
        Args:
            url: 英雄列表页URL
            
        Returns:
            提取的文本内容
        """
        soup, response = self.get_page_content(url)
        if not soup:
            return None
        
        content_parts = []
        seen_texts = set()
        
        # 提取页面标题
        title = soup.find('title')
        if title:
            title_text = self.clean_text(title.get_text())
            if title_text:
                content_parts.append(f"【页面标题】\n{title_text}\n")
        
        # 移除 script 和 style 标签
        for tag in soup.find_all(['script', 'style', 'noscript']):
            tag.decompose()
        
        # 提取所有标题
        for tag_name in ['h1', 'h2', 'h3']:
            tags = soup.find_all(tag_name)
            if tags:
                content_parts.append(f"\n【{tag_name.upper()} 标题】")
                for tag in tags:
                    text = self.clean_text(tag.get_text())
                    if text and text not in seen_texts:
                        content_parts.append(f"  {text}")
                        seen_texts.add(text)
                content_parts.append("")
        
        # 提取英雄链接列表
        hero_links = []
        links = soup.find_all('a', href=True)
        for link in links:
            text = self.clean_text(link.get_text())
            href = link.get('href', '')
            if text and 'herodetail' in href:
                full_url = urljoin(url, href)
                hero_links.append(f"  {text} -> {full_url}")
        
        if hero_links:
            content_parts.append("\n【英雄列表】")
            content_parts.extend(hero_links)
            content_parts.append("")
        
        # 提取其他重要文本
        main_texts = self.extract_text_from_element(soup, min_length=5)
        if main_texts:
            content_parts.append("\n【页面内容】")
            for text in main_texts[:50]:  # 限制数量
                if text not in seen_texts and len(text) > 10:
                    content_parts.append(f"  {text}")
                    seen_texts.add(text)
            content_parts.append("")
        
        return "\n".join(content_parts)
    
    def save_content(self, content, filename, source_url=None):
        """
        保存内容到文件，确保使用 UTF-8 编码
        
        Args:
            content: 要保存的内容
            filename: 文件名
            source_url: 来源URL（可选）
        """
        if not content:
            print(f"  ⚠ 警告: {filename} 没有内容可保存")
            return
        
        filepath = os.path.join(self.output_dir, filename)
        try:
            # 确保使用 UTF-8 编码保存
            with open(filepath, 'w', encoding='utf-8', errors='replace') as f:
                f.write(f"爬取时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
                if source_url:
                    f.write(f"来源URL: {source_url}\n")
                f.write("=" * 80 + "\n\n")
                f.write(content)
            
            # 获取文件大小
            file_size = os.path.getsize(filepath)
            size_kb = file_size / 1024
            print(f"  ✓ 内容已保存: {filename} ({size_kb:.1f} KB)")
        except Exception as e:
            print(f"  ✗ 保存文件失败: {e}")
    
    def scrape_urls(self, urls):
        """
        批量爬取多个URL
        
        Args:
            urls: URL列表，每个元素可以是字符串或字典 {url: str, type: str}
        """
        total = len(urls)
        for idx, item in enumerate(urls, 1):
            if isinstance(item, dict):
                url = item['url']
                page_type = item.get('type', 'detail')
                pre_hero_name = item.get('hero_name')  # 从URL列表中获取的英雄名字
            else:
                url = item
                # 根据URL判断类型
                if 'herolist' in url:
                    page_type = 'list'
                else:
                    page_type = 'detail'
                pre_hero_name = None
            
            print(f"\n{'='*80}")
            if pre_hero_name:
                print(f"[{idx}/{total}] 开始爬取: {pre_hero_name} ({url})")
            else:
                print(f"[{idx}/{total}] 开始爬取: {url}")
            print(f"类型: {page_type}")
            print(f"{'='*80}")
            
            # 根据类型选择爬取方法
            if page_type == 'list':
                content = self.scrape_hero_list(url)
                hero_name = None
            else:
                hero_name, content = self.scrape_hero_detail(url)
                # 如果从页面提取失败，使用预先获取的名字
                if not hero_name and pre_hero_name:
                    hero_name = pre_hero_name
            
            # 生成文件名
            if page_type == 'detail' and hero_name:
                # 使用英雄名字作为文件名
                # 清理文件名中不允许的字符
                safe_name = re.sub(r'[<>:"/\\|?*]', '', hero_name)
                filename_base = safe_name
            else:
                # 回退到使用URL中的文件名
                parsed_url = urlparse(url)
                filename_base = os.path.basename(parsed_url.path).replace('.shtml', '').replace('.html', '')
                if not filename_base or filename_base == '/':
                    filename_base = 'index'
            
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"{filename_base}_{page_type}_{timestamp}.txt"
            
            # 保存内容
            self.save_content(content, filename, source_url=url)
            
            # 延迟，避免请求过快
            if idx < total:
                print(f"  等待 2 秒后继续...")
                time.sleep(2)
        
        print(f"\n{'='*80}")
        print(f"✓ 爬取完成！所有内容已保存到目录:")
        print(f"  {self.output_dir}")
        print(f"{'='*80}\n")


def main():
    """主函数"""
    import sys
    
    scraper = HeroScraper()
    
    # 检查命令行参数
    if len(sys.argv) > 1 and sys.argv[1] == '--all-heroes':
        # 批量爬取所有英雄详情
        print("=" * 80)
        print("开始批量爬取所有英雄详情...")
        print("=" * 80)
        
        # 从英雄列表页面提取所有英雄URL
        list_url = 'https://pvp.qq.com/web201605/herolist.shtml'
        print(f"\n正在从英雄列表页面提取英雄链接: {list_url}")
        hero_urls = scraper.extract_hero_urls_from_list(list_url)
        
        if not hero_urls:
            print("✗ 未能提取到英雄链接")
            return
        
        print(f"✓ 找到 {len(hero_urls)} 个英雄，开始批量爬取...\n")
        
        # 转换为爬取格式，保留英雄名字信息
        urls = []
        for hero_name, hero_url in hero_urls:
            urls.append({
                'url': hero_url,
                'type': 'detail',
                'hero_name': hero_name  # 传递英雄名字
            })
        
        # 批量爬取
        scraper.scrape_urls(urls)
        
    else:
        # 默认爬取指定的URL列表
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
        
        scraper.scrape_urls(urls)


if __name__ == "__main__":
    main()
