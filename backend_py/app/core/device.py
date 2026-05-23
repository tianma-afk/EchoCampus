# app/core/device.py
"""
统一设备管理模块
优先级: CUDA > DirectML > CPU
"""

import torch
import logging

logger = logging.getLogger(__name__)

# 全局设备对象
_device = None
_device_type = None


def get_available_device():
    """
    检测可用的最佳设备
    
    Returns:
        tuple: (device, device_type)
            - device: torch.device 或 torch_directml.device() 对象
            - device_type: str, 可选 'cuda', 'directml', 'cpu'
    """
    global _device, _device_type
    
    # 如果已经初始化过，直接返回缓存
    if _device is not None:
        return _device, _device_type
    
    # 1. 优先检查 NVIDIA CUDA
    if torch.cuda.is_available():
        _device = torch.device('cuda')
        _device_type = 'cuda'
        logger.info(f"✅ 使用 NVIDIA CUDA: {torch.cuda.get_device_name(0)}")
        logger.info(f"   CUDA 版本: {torch.version.cuda}")
        return _device, _device_type
    
    # 2. 检查 DirectML (Windows GPU 通用加速)
    try:
        import torch_directml
        if torch_directml.is_available():
            _device = torch_directml.device()
            _device_type = 'directml'
            logger.info(f"✅ 使用 DirectML GPU 加速")
            logger.info(f"   设备: {_device}")
            return _device, _device_type
    except ImportError:
        logger.debug("torch-directml 未安装")
    except Exception as e:
        logger.warning(f"DirectML 初始化失败: {e}")
    
    # 3. 最终回退到 CPU
    _device = torch.device('cpu')
    _device_type = 'cpu'
    logger.warning("⚠️ 未检测到可用 GPU，使用 CPU 运行（速度较慢）")
    
    return _device, _device_type


def get_device():
    """获取设备对象（兼容旧接口）"""
    device, _ = get_available_device()
    return device


def get_device_type():
    """获取设备类型字符串"""
    _, device_type = get_available_device()
    return device_type


def to_device(data, device=None):
    """
    递归地将数据移动到指定设备
    
    Args:
        data: 可以是 Tensor, dict, list, tuple
        device: 目标设备，如果不指定则使用全局最佳设备
    
    Returns:
        移动到设备后的数据
    """
    if device is None:
        device, _ = get_available_device()
    
    if isinstance(data, torch.Tensor):
        return data.to(device)
    elif isinstance(data, dict):
        return {k: to_device(v, device) for k, v in data.items()}
    elif isinstance(data, (list, tuple)):
        return type(data)(to_device(x, device) for x in data)
    return data


def is_cuda_available():
    """检查 CUDA 是否可用"""
    return torch.cuda.is_available()


def is_directml_available():
    """检查 DirectML 是否可用"""
    try:
        import torch_directml
        return torch_directml.is_available()
    except ImportError:
        return False


def get_device_info():
    """获取详细的设备信息"""
    device, device_type = get_available_device()
    
    info = {
        'device_type': device_type,
        'device': str(device),
    }
    
    if device_type == 'cuda':
        info['cuda_version'] = torch.version.cuda
        info['device_name'] = torch.cuda.get_device_name(0)
        info['device_count'] = torch.cuda.device_count()
    elif device_type == 'directml':
        try:
            import torch_directml
            info['directml_version'] = torch_directml.__version__ if hasattr(torch_directml, '__version__') else 'unknown'
        except:
            pass
    else:
        info['note'] = '使用 CPU，性能较低'
    
    return info


def print_device_info():
    """打印设备信息（用于调试）"""
    info = get_device_info()
    print("\n" + "="*50)
    print("🔧 设备信息:")
    for key, value in info.items():
        print(f"   {key}: {value}")
    print("="*50 + "\n")