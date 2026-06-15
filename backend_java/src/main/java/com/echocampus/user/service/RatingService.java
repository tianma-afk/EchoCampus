package com.echocampus.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.user.vo.RatingVO;

import java.math.BigDecimal;
import java.util.UUID;

public interface RatingService {

    void rate(UUID userId, UUID landmarkId, BigDecimal rating);

    RatingVO getUserRating(UUID userId, UUID landmarkId);

    Page<RatingVO> getUserRatings(UUID userId, int page, int size);
}
