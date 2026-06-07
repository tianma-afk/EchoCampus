import sys
import logging
from pathlib import Path
from loguru import logger

PROJECT_ROOT = Path(__file__).resolve().parents[2]
LOG_DIR = PROJECT_ROOT / "logs"
LOG_DIR.mkdir(exist_ok=True)

# 移除默认 handler
logger.remove()

# 控制台输出 — 带颜色，INFO 级别
logger.add(
    sys.stderr,
    format=(
        "<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> | "
        "<level>{level: <8}</level> | "
        "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - "
        "<level>{message}</level>"
    ),
    level="INFO",
    colorize=True,
)

# 文件输出 — 按天轮转，保留 30 天，DEBUG 级别
logger.add(
    LOG_DIR / "app_{time:YYYY-MM-DD}.log",
    format="{time:YYYY-MM-DD HH:mm:ss.SSS} | {level: <8} | {name}:{function}:{line} - {message}",
    level="DEBUG",
    rotation="00:00",
    retention="30 days",
    encoding="utf-8",
)


class InterceptHandler(logging.Handler):
    """将标准库 logging 重定向到 loguru"""

    def emit(self, record: logging.LogRecord):
        level = logger.level(record.levelname).name if record.levelname in logger._core.levels else record.levelno
        frame, depth = logging.currentframe(), 2
        while frame and frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back
            depth += 1
        logger.opt(depth=depth, exception=record.exc_info).log(level, record.getMessage())


# 拦截标准库 logging，使 uvicorn / fastapi 的日志统一走 loguru
logging.basicConfig(handlers=[InterceptHandler()], level=0, force=True)
