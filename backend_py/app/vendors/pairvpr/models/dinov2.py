# Copyright 2024-present CSIRO.
# Licensed under CSIRO BSD-3-Clause-Clear (Non-Commercial)
#


import torch
import torch.nn as nn
import os
from pathlib import Path

os.environ['TORCH_HOME'] = str(Path(__file__).resolve().parents[4] / 'weights')
DINOV2_ARCHS = {
    'dinov2_vits14': 384,
    'dinov2_vitb14': 768,
    'dinov2_vitl14': 1024,
    'dinov2_vitg14': 1536,
    'dinov2_vits14_reg': 384,
    'dinov2_vitb14_reg': 768,
    'dinov2_vitl14_reg': 1024,
    'dinov2_vitg14_reg': 1536,
}

class DINOv2(nn.Module):
    """
    DINOv2 model via torch hub.

    Args:
        model_name: The code for the model architecture e.g. 'dinov2_vitb14_reg', 'dinov2_vitg14'
        num_trainable_blocks: The number of last blocks in the model that are trainable.
        return_classtok: If True, the forward pass returns both the class token and feature map.
    """

    def __init__(self, model_name:str='dinov2_vitb14', num_trainable_blocks:int=2, 
                 return_classtok:bool=True):
        super().__init__()

        assert model_name in DINOV2_ARCHS.keys(), f'Unknown model name {model_name}'
        _DINOV2_REPO = str(Path(__file__).resolve().parents[4] / 'weights' / 'hub' / 'facebookresearch_dinov2_main')
        _DINOV2_MODEL = str(Path(__file__).resolve().parents[4] / 'weights' / 'hub' / 'checkpoints' / f'{model_name}_pretrain.pth')
        if not Path(_DINOV2_REPO).is_dir():
            raise FileNotFoundError(
                f"DINOv2 仓库目录不存在，请先运行下载脚本:\n"
                f"  uv run python scripts/download_models.py\n"
                f"期望路径: {_DINOV2_REPO}"
            )
        if not Path(_DINOV2_MODEL).is_file():
            raise FileNotFoundError(
                f"DINOv2 预训练权重文件不存在，请先运行下载脚本:\n"
                f"  uv run python scripts/download_models.py\n"
                f"期望路径: {_DINOV2_MODEL}"
            )
        self.model = torch.hub.load(_DINOV2_REPO, model_name, source='local')
        self.num_channels = DINOV2_ARCHS[model_name]
        self.num_trainable_blocks = num_trainable_blocks
        self.return_classtok = return_classtok

    def forward(self, x, mask=None):
        """
        The forward method for DINOv2.

        Parameters:
            x: The input tensor [B, 3, H, W]. H and W must be be divisible by 14.
            mask: An optional mask, used during stage one training.

        Returns:
            f: The feature map.
            c: The class token [B, C].
        """

        x = self.model.prepare_tokens_with_masks(x, mask)
        
        if self.num_trainable_blocks == 0:
            # Freeze entire Dino model
            with torch.no_grad():
                for blk in self.model.blocks:
                    x = blk(x)
            x = x.detach()
        else:
            # First Dino blocks are frozen
            with torch.no_grad():
                for blk in self.model.blocks[:-self.num_trainable_blocks]:
                    x = blk(x)
            x = x.detach()

            # Last Dino blocks are unfrozen
            for blk in self.model.blocks[-self.num_trainable_blocks:]:
                x = blk(x)

        x = self.model.norm(x)
        
        c = x[:, 0] # class token
        r = x[:, 1 : self.model.num_register_tokens + 1] # register tokens (unused)
        f = x[:, self.model.num_register_tokens + 1 :] # feature tokens

        if self.return_classtok:
            return f, c
        return f
