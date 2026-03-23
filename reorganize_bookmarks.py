#!/usr/bin/env python3
"""
智能重组Chrome收藏夹 - 按逻辑分类
"""
import re
import html
from pathlib import Path
from collections import defaultdict

# ============ 智能分类映射 ============
# 旧文件夹路径(部分匹配) -> 新一级分类/二级分类
FOLDER_MAPPING = {
    # 个人生活
    "影视社交": ("01_个人生活", "影视社交"),
    "健康运动": ("01_个人生活", "健康运动"),
    "兴趣爱好生活": ("01_个人生活", "兴趣爱好"),
    "temp-吉他谱": ("01_个人生活", "兴趣爱好"),
    "书籍-出版社": ("01_个人生活", "书籍阅读"),
    "励志鸡汤": ("01_个人生活", "励志成长"),
    "我的应用2": ("01_个人生活", "常用应用"),
    
    # 家庭与教育
    "梓豪入学": ("02_家庭与教育", "子女入学"),
    "英著梦霞入学": ("02_家庭与教育", "子女入学"),
    "孩子教育": ("02_家庭与教育", "子女教育"),
    "育儿": ("02_家庭与教育", "育儿"),
    "国外文章": ("02_家庭与教育", "育儿"),  # 育儿下的
    "学校教育": ("02_家庭与教育", "学校教育"),
    "学历": ("02_家庭与教育", "学历考试"),
    "成考-刷题": ("02_家庭与教育", "学历考试"),
    "term1备考": ("02_家庭与教育", "学历考试"),
    "茂名初中生教材": ("02_家庭与教育", "学历考试"),
    "学生网址": ("02_家庭与教育", "学历考试"),
    "梓豪": ("02_家庭与教育", "子女教育"),
    "深大-作业习题": ("02_家庭与教育", "学历考试"),
    "计算机网络": ("02_家庭与教育", "学历考试"),  # 深大下的
    
    # 职业发展
    "招聘": ("03_职业发展", "招聘求职"),
    "入深户": ("03_职业发展", "入户手续"),
    "职业经理人": ("03_职业发展", "职场进阶"),
    "管理学": ("03_职业发展", "职场进阶"),
    "公务员": ("03_职业发展", "公考"),
    
    # 医疗护理 (djl)
    "djl": ("04_医疗护理", "卫生职称"),
    "ppt题材": ("04_医疗护理", "PPT素材"),
    "2024中级护师报考": ("04_医疗护理", "护师报考"),
    
    # 财经商业
    "财经资讯": ("05_财经商业", "财经资讯"),
    "最近": ("05_财经商业", "财经资讯"),  # 财经下的最近
    "国外财经": ("05_财经商业", "国际财经"),
    "投资理财房产": ("05_财经商业", "投资理财"),
    "人物专访": ("05_财经商业", "人物专访"),
    "视野": ("05_财经商业", "国际视野"),
    "主机选购": ("05_财经商业", "硬件选购"),
    "福彩": ("05_财经商业", "其他"),
    
    # 电商跨境
    "电商": ("06_电商跨境", "跨境电商"),
    "虾皮": ("06_电商跨境", "平台"),
    "跨境电商资讯": ("06_电商跨境", "资讯"),
    "亚马逊": ("06_电商跨境", "平台"),
    "买家地址": ("06_电商跨境", "平台"),
    "速卖通": ("06_电商跨境", "平台"),
    "营销工具": ("06_电商跨境", "工具"),
    "俄罗斯电商研究": ("06_电商跨境", "市场研究"),
    "国家行业政策": ("06_电商跨境", "政策法规"),
    "行业研究": ("06_电商跨境", "行业研究"),
    "玩具": ("06_电商跨境", "行业研究"),
    "new_越南": ("06_电商跨境", "市场研究"),
    
    # 公司商务
    "公司": ("07_公司商务", "企业信用"),
    "清洁公司": ("07_公司商务", "企业信用"),
    "公司-法务等": ("07_公司商务", "法务"),
    "知识产权": ("07_公司商务", "法务"),
    "汽车交规": ("07_公司商务", "交通法规"),
    
    # 创作与设计
    "创作渠道": ("08_创作与设计", "创作平台"),
    "国外新闻-transfor": ("08_创作与设计", "素材"),
    "ai": ("08_创作与设计", "AI素材"),
    "变现": ("08_创作与设计", "变现"),
    "特效制作": ("08_创作与设计", "视频特效"),
    "after-effets": ("08_创作与设计", "视频特效"),
    "ps": ("08_创作与设计", "平面设计"),
    "chatGPT": ("08_创作与设计", "AI工具"),
    "星座": ("08_创作与设计", "素材"),  # 星座放创作(做内容用)
    
    # 开发技术 - tech1/tech2/官网/在线教育 合并
    "github阅读": ("09_开发技术", "GitHub"),
    "算法": ("09_开发技术", "算法"),
    "java": ("09_开发技术", "Java"),
    "linux": ("09_开发技术", "Linux"),
    "数据库": ("09_开发技术", "数据库"),
    "mongdb": ("09_开发技术", "数据库"),
    "mysql": ("09_开发技术", "数据库"),
    "common": ("09_开发技术", "数据库"),
    "分库分表": ("09_开发技术", "数据库"),
    "数据库中间件": ("09_开发技术", "数据库"),
    "redis": ("09_开发技术", "Redis"),
    "前端": ("09_开发技术", "前端"),
    "html": ("09_开发技术", "前端"),
    "js": ("09_开发技术", "前端"),
    "css": ("09_开发技术", "前端"),
    "h5": ("09_开发技术", "前端"),
    "jquery": ("09_开发技术", "前端"),
    "vue": ("09_开发技术", "前端"),
    "node": ("09_开发技术", "前端"),
    "json": ("09_开发技术", "前端"),
    "flutter": ("09_开发技术", "前端"),
    "前端打包工具": ("09_开发技术", "前端"),
    "webpack": ("09_开发技术", "前端"),
    "包管理器npm": ("09_开发技术", "前端"),
    "不同语法区别": ("09_开发技术", "前端"),
    "python": ("09_开发技术", "Python"),
    "运维": ("09_开发技术", "运维"),
    "nexus": ("09_开发技术", "运维"),
    "版本管理": ("09_开发技术", "运维"),
    "产品设计": ("09_开发技术", "产品设计"),
    "源码学习": ("09_开发技术", "源码学习"),
    "tomcat": ("09_开发技术", "源码学习"),
    "spring": ("09_开发技术", "源码学习"),  # 源码下的
    "mybatis": ("09_开发技术", "源码学习"),  # 源码下的
    "游戏开发": ("09_开发技术", "游戏开发"),
    "cocos": ("09_开发技术", "游戏开发"),
    "Unity": ("09_开发技术", "游戏开发"),
    "idea激活": ("09_开发技术", "开发工具"),
    "技术管理": ("09_开发技术", "技术管理"),
    "国内": ("09_开发技术", "技术社区"),
    "国外技术流行站点": ("09_开发技术", "技术社区"),
    "个人技术博客": ("09_开发技术", "技术博客"),
    "个人博客2": ("09_开发技术", "技术博客"),
    "国外名人博客": ("09_开发技术", "技术博客"),
    "代码网站": ("09_开发技术", "代码资源"),
    "观点&资讯": ("09_开发技术", "技术资讯"),
    "面试": ("09_开发技术", "面试准备"),
    "精读": ("09_开发技术", "精读"),
    "如果提高编程技巧": ("09_开发技术", "编程提升"),
    "付费": ("09_开发技术", "付费资源"),
    "代码规范和管理": ("09_开发技术", "工程实践"),
    "架构师": ("09_开发技术", "架构进阶"),
    "java最新路线": ("09_开发技术", "学习路线"),
    "程序员软能力": ("09_开发技术", "软技能"),
    "在线电子书": ("09_开发技术", "电子书"),
    "云服务器": ("09_开发技术", "云服务"),
    "阿里巴巴": ("09_开发技术", "大厂文档"),
    "阿里电子书": ("09_开发技术", "电子书"),
    "腾讯": ("09_开发技术", "大厂文档"),
    "jdk_doc": ("09_开发技术", "官方文档"),
    "apache": ("09_开发技术", "官方文档"),
    "消息队列": ("09_开发技术", "消息队列"),
    "其他": ("09_开发技术", "其他框架"),
    "微信开发": ("09_开发技术", "微信开发"),
    "k8s": ("09_开发技术", "运维"),
    "docker": ("09_开发技术", "运维"),
    "硬件": ("09_开发技术", "硬件"),
    "监控": ("09_开发技术", "运维"),
    "prometheus": ("09_开发技术", "运维"),
    "go": ("09_开发技术", "Go"),
    # base&middle, senior, 框架等归入Java子类
    "base&middle": ("09_开发技术", "Java"),
    "springmvc": ("09_开发技术", "Java"),
    "senior": ("09_开发技术", "Java"),
    "java阅读": ("09_开发技术", "Java"),
    "java并发相关": ("09_开发技术", "Java"),
    "工作流": ("09_开发技术", "Java"),
    "框架": ("09_开发技术", "Java"),
    "springboot": ("09_开发技术", "Java"),
    "mybatis": ("09_开发技术", "Java"),  # 框架下的
    "高级架构": ("09_开发技术", "Java"),
    "dubbo": ("09_开发技术", "Java"),
    "杂": ("09_开发技术", "Java"),
    "构建工具": ("09_开发技术", "Java"),
    "maven": ("09_开发技术", "Java"),
    "gradle": ("09_开发技术", "Java"),
    "常见功能案例": ("09_开发技术", "Java"),
    
    # 在线学习
    "外语学习": ("10_在线学习", "外语"),
    "采访": ("10_在线学习", "外语"),
    "英语2": ("10_在线学习", "外语"),
    "英语3": ("10_在线学习", "外语"),
    "在线教程": ("10_在线学习", "IT教程"),
    "imoc": ("10_在线学习", "IT教程"),
    "2025": ("10_在线学习", "IT教程"),
    "在线读书": ("10_在线学习", "电子书"),
    
    # 工具 (合并 工具+工具2)
    "常用搜索&翻译": ("11_工具", "搜索翻译"),
    "ChatGPT": ("11_工具", "AI助手"),
    "gpt常用": ("11_工具", "AI助手"),
    "gpt2": ("11_工具", "AI助手"),
    "在线工具": ("11_工具", "在线工具"),
    "在线画图": ("11_工具", "在线工具"),
    "各种激活": ("11_工具", "激活工具"),
    "开发相关工具": ("11_工具", "开发工具"),
    "eclipse": ("11_工具", "开发工具"),
    "git": ("11_工具", "开发工具"),
    "intelliJ IDEA": ("11_工具", "开发工具"),
    "PyCharm": ("11_工具", "开发工具"),
    "powerDesinger": ("11_工具", "开发工具"),
    "jira&bitbucket": ("11_工具", "开发工具"),
    "navicat": ("11_工具", "开发工具"),
    "vscode": ("11_工具", "开发工具"),
    "JS工具": ("11_工具", "前端工具"),
    "萌芽网站": ("11_工具", "开发工具"),
    "电脑选配": ("11_工具", "硬件"),
    "公众号爬取付费": ("11_工具", "工具"),
    "pdf转word": ("11_工具", "文档工具"),
    
    # 工作 (Com_, recently, work_tmp 保持)
    "zt": ("12_工作", "纵腾-zt"),
    "mytest_prometheus": ("12_工作", "测试环境"),
    "pas": ("12_工作", "项目-pas"),
    "home_local": ("12_工作", "本地环境"),
    "lph": ("12_工作", "项目-lph"),
    "recently": ("12_工作", "待办与阅读"),
    "recently_todo1": ("12_工作", "待办与阅读"),
    "dailyTask_todo": ("12_工作", "待办与阅读"),
    "源码阅读任务": ("12_工作", "待办与阅读"),
    "book_online": ("12_工作", "待办与阅读"),
    "马士兵视频": ("12_工作", "待办与阅读"),
    "腾讯云社区temp": ("12_工作", "待办与阅读"),
    "Pixus_daliyTodo": ("12_工作", "待办与阅读"),
    "game_server": ("12_工作", "技术专题"),
    "netty+websocket": ("12_工作", "技术专题"),
    "es": ("12_工作", "技术专题"),
    "work_tmp": ("12_工作", "临时文档"),
}

# 书签栏根目录下直接的书签 (网盘、读书等) -> 个人生活/常用应用
ROOT_BOOKMARKS_CATEGORY = ("01_个人生活", "常用应用")

# tech1, tech2, 官网, 在线教育 顶层文件夹名 -> 合并到 09_开发技术 或 10_在线学习
TOP_LEVEL_MAPPING = {
    "书签栏": None,  # 其子项已单独映射
    "我的应用": None,  # 子项已映射
    "tech1": ("09_开发技术", "核心技术"),
    "tech2": ("09_开发技术", "社区与面试"),
    "官网": ("09_开发技术", "官方文档"),
    "在线教育": ("10_在线学习", "IT学习平台"),
    "Com_": ("12_工作", "公司项目"),
    "工具": ("11_工具", "综合"),
    "工具2": ("11_工具", "AI工具"),
    "recently": ("12_工作", "待办与阅读"),
    "work_tmp": ("12_工作", "临时文档"),
}


def parse_bookmarks_html(path: Path) -> tuple[list, list]:
    """解析HTML，返回 (所有书签列表, 原始HTML块)"""
    with open(path, encoding="utf-8", errors="replace") as f:
        content = f.read()
    
    bookmarks = []  # (url, title, add_date, icon, folder_path_list)
    # 使用正则提取
    # 匹配 <DT><A HREF="..." ADD_DATE="..." ICON="...">title</A>
    a_pattern = re.compile(
        r'<DT><A\s+HREF="([^"]*)"\s+ADD_DATE="(\d*)"(?:\s+ICON="([^"]*)")?>([^<]*)</A>',
        re.DOTALL
    )
    
    # 匹配文件夹结构：需要逐层解析
    # 简化：我们按顺序扫描，维护当前路径栈
    lines = content.split('\n')
    stack = []  # [(indent, name), ...]
    current_path = []
    
    i = 0
    while i < len(lines):
        line = lines[i]
        # 检测 H3 文件夹
        h3_match = re.search(r'<DT><H3[^>]*>([^<]+)</H3>', line)
        if h3_match:
            name = html.unescape(h3_match.group(1).strip())
            # 计算缩进层级
            indent = len(line) - len(line.lstrip())
            # 弹栈到同级或上级
            while stack and stack[-1][0] >= indent:
                stack.pop()
                if current_path:
                    current_path.pop()
            stack.append((indent, name))
            current_path.append(name)
            i += 1
            continue
        
        # 检测 </DL> 结束
        if '</DL>' in line and '<DT>' not in line:
            indent = len(line) - len(line.lstrip())
            while stack and stack[-1][0] >= indent:
                stack.pop()
                if current_path:
                    current_path.pop()
            i += 1
            continue
        
        # 检测书签
        a_match = a_pattern.search(line)
        if a_match and current_path:
            url, add_date, icon, title = a_match.groups()
            title = html.unescape(title.strip()) if title else url
            bookmarks.append({
                "url": url,
                "title": title,
                "add_date": add_date,
                "icon": icon or "",
                "folder_path": list(current_path),
            })
        
        i += 1
    
    return bookmarks


def get_new_category(folder_path: list) -> tuple[str, str]:
    """根据原始路径返回 (一级分类, 二级分类)"""
    # 从最具体到最泛匹配
    for i in range(len(folder_path) - 1, -1, -1):
        name = folder_path[i]
        if name in FOLDER_MAPPING:
            return FOLDER_MAPPING[name]
    
    # 顶层文件夹
    if folder_path:
        top = folder_path[0]
        if top in TOP_LEVEL_MAPPING:
            mapping = TOP_LEVEL_MAPPING[top]
            if mapping:
                if len(folder_path) == 1:
                    return mapping
                return (mapping[0], mapping[1] + "/" + "/".join(folder_path[1:]))
        
        # 书签栏下的未映射项
        if top == "书签栏":
            if len(folder_path) == 1:
                return ROOT_BOOKMARKS_CATEGORY
            second = folder_path[1] if len(folder_path) > 1 else ""
            if second == "我的应用" and len(folder_path) == 2:
                return ("01_个人生活", "常用应用")
            if second in FOLDER_MAPPING:
                return FOLDER_MAPPING[second]
            # 我的应用下的未映射
            if second == "我的应用":
                return ("01_个人生活", "其他")
            # 其他书签栏直接子项
            for k, v in FOLDER_MAPPING.items():
                if k in second or second in (f[1] for f in FOLDER_MAPPING.values()):
                    break
            return ("01_个人生活", second if second else "其他")
        
        # tech1, tech2 等下的未映射子文件夹
        if top in ("tech1", "tech2", "官网"):
            return ("09_开发技术", "/".join(folder_path[1:]) if len(folder_path) > 1 else "其他")
        if top == "在线教育":
            return ("10_在线学习", "/".join(folder_path[1:]) if len(folder_path) > 1 else "其他")
        if top == "Com_":
            return ("12_工作", "/".join(folder_path) if folder_path else "其他")
        if top in ("工具", "工具2"):
            return ("11_工具", "/".join(folder_path[1:]) if len(folder_path) > 1 else "综合")
    
    return ("00_未分类", "其他")


def build_html(bookmarks_by_category: dict) -> str:
    """根据分类构建新的HTML"""
    header = """<!DOCTYPE NETSCAPE-Bookmark-file-1>
<!-- 智能重组后的书签 - 按逻辑分类 -->
<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
<TITLE>Bookmarks</TITLE>
<H1>Bookmarks</H1>
<DL><p>
    <DT><H3 ADD_DATE="1768880000" LAST_MODIFIED="1768880000" PERSONAL_TOOLBAR_FOLDER="true">书签栏</H3>
    <DL><p>
"""
    
    categories_order = [
        "01_个人生活", "02_家庭与教育", "03_职业发展", "04_医疗护理",
        "05_财经商业", "06_电商跨境", "07_公司商务", "08_创作与设计",
        "09_开发技术", "10_在线学习", "11_工具", "12_工作", "00_未分类"
    ]
    
    body_parts = []
    for cat1 in categories_order:
        if cat1 not in bookmarks_by_category:
            continue
        subcats = bookmarks_by_category[cat1]
        # 按二级分类名排序
        for cat2 in sorted(subcats.keys()):
            bms = subcats[cat2]
            if not bms:
                continue
            # 文件夹名：去掉数字前缀和_ 显示更友好
            display_name = cat1.replace("00_", "").replace("01_", "").replace("02_", "").replace("03_", "").replace("04_", "").replace("05_", "").replace("06_", "").replace("07_", "").replace("08_", "").replace("09_", "").replace("10_", "").replace("11_", "").replace("12_", "")
            folder_name = f"{display_name} - {cat2}" if cat2 and cat2 != "其他" else display_name
            body_parts.append(f'        <DT><H3 ADD_DATE="1768880000" LAST_MODIFIED="1768880000">{html.escape(folder_name)}</H3>\n')
            body_parts.append('        <DL><p>\n')
            for bm in bms:
                icon_attr = f' ICON="{html.escape(bm["icon"])}"' if bm.get("icon") else ""
                body_parts.append(f'            <DT><A HREF="{html.escape(bm["url"])}" ADD_DATE="{bm["add_date"]}"{icon_attr}>{html.escape(bm["title"])}</A>\n')
            body_parts.append('        </DL><p>\n')
    
    footer = """    </DL><p>
</DL><p>
"""
    
    return header + "".join(body_parts) + footer


def main():
    base = Path(__file__).parent
    src = base / "bookmarks_2026_2_27.html"
    if not src.exists():
        print(f"错误: 找不到 {src}")
        return 1
    
    print("正在解析收藏夹...")
    bookmarks = parse_bookmarks_html(src)
    print(f"共解析到 {len(bookmarks)} 个书签")
    
    # 按新分类聚集
    by_category = defaultdict(lambda: defaultdict(list))
    unmapped_paths = set()
    
    for bm in bookmarks:
        cat1, cat2 = get_new_category(bm["folder_path"])
        by_category[cat1][cat2].append(bm)
        if cat1 == "00_未分类":
            unmapped_paths.add("/".join(bm["folder_path"]))
    
    if unmapped_paths:
        print("未映射的路径示例:", list(unmapped_paths)[:5])
    
    # 生成HTML
    html_content = build_html(by_category)
    out_path = base / "bookmarks_重组_智能分类.html"
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(html_content)
    
    print(f"\n已生成: {out_path}")
    
    # 输出分类概览
    print("\n新分类概览:")
    for cat1 in ["01_个人生活", "02_家庭与教育", "03_职业发展", "04_医疗护理",
                 "05_财经商业", "06_电商跨境", "07_公司商务", "08_创作与设计",
                 "09_开发技术", "10_在线学习", "11_工具", "12_工作"]:
        if cat1 in by_category:
            total = sum(len(bms) for bms in by_category[cat1].values())
            print(f"  {cat1}: {total} 个书签, {len(by_category[cat1])} 个子类")
    
    return 0


if __name__ == "__main__":
    exit(main())
