package com.echocampus.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.user.vo.FavoriteVO;

import java.util.List;
import java.util.UUID;

public interface FavoriteService {

    /**
     * Toggle favorite. Returns true if now favorited, false if unfavorited.
     */
    boolean toggleFavorite(UUID userId, UUID landmarkId);

    Page<FavoriteVO> getFavorites(UUID userId, int page, int size);

    boolean isFavorited(UUID userId, UUID landmarkId);

    void batchDelete(UUID userId, List<UUID> ids);
}
