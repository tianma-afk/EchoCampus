import tkinter as tk
from tkinter import filedialog, messagebox
import os
import ctypes
import json
import sys


# 配置文件路径 (保存在temp_resources下)
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG_FILE = os.path.join(PROJECT_ROOT, "temp_resources", "user_config.json")

def load_last_path():
    """读取上次使用的路径"""
    if not os.path.exists(CONFIG_FILE):
        return None
    try:
        with open(CONFIG_FILE, 'r', encoding='utf-8') as f:
            data = json.load(f)
            path = data.get("last_file_path")
            if path and os.path.exists(path):
                return path
    except Exception:
        pass
    return None

def save_last_path(file_path):
    """保存当前选择的路径"""
    try:
        with open(CONFIG_FILE, 'w', encoding='utf-8') as f:
            json.dump({"last_file_path": file_path}, f, ensure_ascii=False, indent=4)
    except Exception as e:
        print(f"⚠️ 保存配置失败: {e}")

def select_single_file() -> str | None:
    """
    智能文件选择器：
    1. 首次运行默认打开【桌面】
    2. 后续运行默认打开【上一次选择的文件所在目录】，并选中该文件
    3. 限制单选
    4. 取消时询问【重试】或【退出】
    """
    
    # --- 1. DPI 修复 ---
    try:
        ctypes.windll.shcore.SetProcessDpiAwareness(1)
    except Exception:
        try:
            ctypes.windll.user32.SetProcessDPIAware()
        except Exception:
            pass
    
    # 获取上次的路径
    last_file = load_last_path()
    
    while True:
        root = tk.Tk()
        root.withdraw()
        root.attributes('-topmost', True)

        # --- 2. 确定初始目录和初始文件 ---
        initial_dir = os.path.join(os.path.expanduser("~"), "Desktop") # 默认桌面
        initial_file = ""
        
        if last_file and os.path.exists(last_file):
            initial_dir = os.path.dirname(last_file)
            initial_file = os.path.basename(last_file)
            print(f"💡 智能记忆：已定位到上次目录 -> {initial_dir}")
        else:
            print("ℹ️ 无历史记录，默认打开桌面。")

        # --- 3. 打开对话框 ---
        file_path = filedialog.askopenfilename(
            title="请选择一个图片文件",
            initialdir=initial_dir,
            initialfile=initial_file,  # 自动选中上次的文件
            filetypes=[
                ("图片文件", "*.jpg *.jpeg *.png *.bmp *.gif *.webp"),
                ("所有文件", "*.*")
            ]
        )

        root.destroy()
        
        # --- 4. 处理结果 ---
        if file_path:
            print(f"✅ 已选择: {file_path}")
            save_last_path(file_path)  # 🌟 关键：保存新路径供下次使用
            return file_path
        
        # 用户取消
        ask_root = tk.Tk()
        ask_root.withdraw()
        ask_root.attributes('-topmost', True)
        
        choice = messagebox.askyesno(
            title="未选择文件",
            message="您没有选择任何文件。\n\n点击【是】重新选择\n点击【否】退出程序",
            icon=messagebox.WARNING
        )
        ask_root.destroy()
        
        if not choice:
            print("🛑 用户选择退出。")
            return None
        # 否则继续循环重试

def get_saved_path():
    """
    获取保存的路径
    """
    config_file = CONFIG_FILE
    
    # 1. 检查文件是否存在
    if not os.path.exists(config_file):
        print("❌ 配置文件不存在")
        return None
    
    try:
        # 2. 打开并读取 JSON
        with open(config_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
            
        # 3. 获取具体字段
        file_path = data.get("last_file_path")
        
        # 4. (可选) 再次确认文件真的还在硬盘上
        if file_path and os.path.exists(file_path):
            return file_path
        else:
            print("⚠️ 配置中有路径，但文件已被删除或移动")
            return None
            
    except Exception as e:
        print(f"❌ 读取失败: {e}")
        return None        

if __name__ == "__main__":
    print("🚀 启动智能文件选择器...")
    selected_file = select_single_file()
    
    if selected_file:
        print("\n>>> 准备处理:", selected_file)
        # run_pipeline(selected_file)
    else:
        print("\n>>> 程序已退出。")
        sys.exit(0)