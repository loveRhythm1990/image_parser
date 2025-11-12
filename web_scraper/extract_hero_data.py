#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从爬取的英雄详情文件中提取关键信息并保存为JSON
"""

import os
import re
import json
from pathlib import Path


def extract_hero_info(file_path):
    """从英雄详情文件中提取信息"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        lines = content.split('\n')
        
        # 初始化数据结构
        hero_data = {
            'name': '',
            'title': '',
            'skill4_name': '',
            'skill4_cooldown': '',
            'skill4_cost': '',
            'skill4_description': ''
        }
        
        # 提取英雄名称
        for line in lines:
            if line.startswith('英雄名称：'):
                hero_data['name'] = line.replace('英雄名称：', '').strip()
                break
        
        # 提取英雄称号
        for line in lines:
            if '【英雄称号】' in line:
                # 称号可能在同一行或下一行
                title = line.replace('【英雄称号】', '').strip()
                if title:
                    hero_data['title'] = title
                else:
                    idx = lines.index(line)
                    if idx + 1 < len(lines):
                        hero_data['title'] = lines[idx + 1].strip()
                break
        
        # 提取技能4信息
        skill4_start_idx = -1
        for i, line in enumerate(lines):
            if line.startswith('技能 4：'):
                skill4_start_idx = i
                # 提取技能名称
                hero_data['skill4_name'] = line.replace('技能 4：', '').strip()
                break
        
        if skill4_start_idx != -1:
            # 提取冷却值
            if skill4_start_idx + 1 < len(lines):
                cooldown_line = lines[skill4_start_idx + 1]
                if '冷却值：' in cooldown_line:
                    cooldown_str = cooldown_line.replace('冷却值：', '').strip()
                    # 提取第一个值（可能是 "40/36/32" 格式，取第一个）
                    if '/' in cooldown_str:
                        hero_data['skill4_cooldown'] = cooldown_str.split('/')[0].strip()
                    else:
                        hero_data['skill4_cooldown'] = cooldown_str
            
            # 提取消耗
            if skill4_start_idx + 2 < len(lines):
                cost_line = lines[skill4_start_idx + 2]
                if '消耗：' in cost_line:
                    hero_data['skill4_cost'] = cost_line.replace('消耗：', '').strip()
            
            # 提取描述（可能跨多行）
            desc_start_idx = -1
            for i in range(skill4_start_idx + 1, len(lines)):
                if '描述：' in lines[i]:
                    desc_start_idx = i
                    break
            
            if desc_start_idx != -1:
                # 收集描述内容，直到遇到分隔线或空行
                description_parts = []
                desc_line = lines[desc_start_idx].replace('描述：', '').strip()
                if desc_line:
                    description_parts.append(desc_line)
                
                # 继续读取后续行
                for i in range(desc_start_idx + 1, len(lines)):
                    line = lines[i].strip()
                    # 如果遇到分隔线或新的标题，停止
                    if line.startswith('---') or line.startswith('【') or line.startswith('技能 '):
                        break
                    # 空行也停止
                    if not line:
                        break
                    description_parts.append(line)
                
                hero_data['skill4_description'] = ''.join(description_parts)
        
        return hero_data
    
    except Exception as e:
        print(f"✗ 处理文件 {file_path} 时出错: {e}")
        return None


def main():
    """主函数"""
    # 设置路径
    scraped_dir = Path(__file__).parent / 'scraped_content'
    output_dir = Path(__file__).parent / 'hero_data'
    output_file = output_dir / 'heroes_skill4_data.json'
    
    print("=" * 80)
    print("开始提取英雄技能4信息...")
    print("=" * 80)
    
    # 查找所有英雄详情文件
    detail_files = list(scraped_dir.glob('*_detail_*.txt'))
    
    if not detail_files:
        print("✗ 未找到英雄详情文件")
        return
    
    print(f"\n找到 {len(detail_files)} 个英雄详情文件")
    
    # 提取所有英雄信息
    heroes_data = []
    success_count = 0
    
    for file_path in sorted(detail_files):
        hero_info = extract_hero_info(file_path)
        if hero_info and hero_info['name']:
            heroes_data.append(hero_info)
            success_count += 1
            print(f"  ✓ {hero_info['name']}")
        else:
            print(f"  ✗ 跳过: {file_path.name}")
    
    # 保存为JSON
    if heroes_data:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(heroes_data, f, ensure_ascii=False, indent=2)
        
        print(f"\n{'='*80}")
        print(f"✓ 成功提取 {success_count} 个英雄的信息")
        print(f"✓ JSON文件已保存: {output_file}")
        print(f"  文件大小: {output_file.stat().st_size / 1024:.1f} KB")
        print(f"{'='*80}\n")
    else:
        print("\n✗ 未能提取任何英雄信息")


if __name__ == "__main__":
    main()

