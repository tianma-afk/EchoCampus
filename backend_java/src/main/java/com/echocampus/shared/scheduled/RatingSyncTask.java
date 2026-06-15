package com.echocampus.shared.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.user.entity.Rating;
import com.echocampus.user.mapper.RatingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RatingSyncTask {

    private static final String LAST_SYNC_KEY = "rating:sync:last_time";

    private final RatingMapper ratingMapper;
    private final LandmarkMapper landmarkMapper;
    private final StringRedisTemplate redisTemplate;

    public RatingSyncTask(RatingMapper ratingMapper, LandmarkMapper landmarkMapper,
                          StringRedisTemplate redisTemplate) {
        this.ratingMapper = ratingMapper;
        this.landmarkMapper = landmarkMapper;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 3_600_000)
    public void syncRating() {
        log.info("[同步评分] 开始增量同步地标平均评分");
        try {
            String lastSyncStr = redisTemplate.opsForValue().get(LAST_SYNC_KEY);
            OffsetDateTime lastSync;
            if (lastSyncStr != null && !lastSyncStr.isEmpty()) {
                lastSync = OffsetDateTime.parse(lastSyncStr);
            } else {
                lastSync = OffsetDateTime.parse("2000-01-01T00:00:00+08:00");
                log.info("[同步评分] 首次同步，全量计算");
            }

            String nowStr = OffsetDateTime.now().toString();

            List<Rating> changedRatings = ratingMapper.selectList(new LambdaQueryWrapper<Rating>()
                    .select(Rating::getLandmarkId)
                    .gt(Rating::getUpdatedAt, lastSync));

            if (changedRatings.isEmpty()) {
                log.info("[同步评分] 无变化的评分记录");
                redisTemplate.opsForValue().set(LAST_SYNC_KEY, nowStr);
                return;
            }

            List<UUID> changedLandmarkIds = changedRatings.stream()
                    .map(Rating::getLandmarkId)
                    .distinct()
                    .collect(Collectors.toList());

            log.info("[同步评分] 检测到 {} 个地标有新的评分记录", changedLandmarkIds.size());

            List<Rating> allRatings = ratingMapper.selectList(new LambdaQueryWrapper<Rating>()
                    .in(Rating::getLandmarkId, changedLandmarkIds));

            Map<UUID, BigDecimal> avgMap = allRatings.stream()
                    .collect(Collectors.groupingBy(
                            Rating::getLandmarkId,
                            Collectors.collectingAndThen(
                                    Collectors.averagingDouble(r -> r.getRating().doubleValue()),
                                    avg -> BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP))));

            int updated = 0;
            for (Map.Entry<UUID, BigDecimal> entry : avgMap.entrySet()) {
                LandmarkEntity landmark = new LandmarkEntity();
                landmark.setId(entry.getKey());
                landmark.setRating(entry.getValue());
                landmarkMapper.updateById(landmark);
                updated++;
            }

            redisTemplate.opsForValue().set(LAST_SYNC_KEY, nowStr);
            log.info("[同步评分] 完成, 更新 {} 个地标, 共计算 {} 条评分", updated, allRatings.size());
        } catch (Exception e) {
            log.error("[同步评分] 失败", e);
        }
    }
}
