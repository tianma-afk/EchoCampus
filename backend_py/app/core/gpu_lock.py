import asyncio

# GPU 全局锁，确保同一时间只有一个任务在使用 GPU
gpu_lock = asyncio.Lock()