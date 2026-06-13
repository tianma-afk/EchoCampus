package com.echocampus.shared.scheduled;

import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.shared.config.RedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HotRankingSyncTask {

    private static final String HOT_RANKING_KEY = "landmark:hot:ranking";

    private final LandmarkMapper landmarkMapper;
    private final StringRedisTemplate redisTemplate;

    public HotRankingSyncTask(LandmarkMapper landmarkMapper, StringRedisTemplate redisTemplate) {
        this.landmarkMapper = landmarkMapper;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 3_600_000)
    public void syncHotRanking() {
        log.info("[同步热门排行榜] 开始从 PostgreSQL 同步打卡数据到 Redis ZSET");
        try {
            var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LandmarkEntity>()
                    .select(LandmarkEntity::getId, LandmarkEntity::getCheckInCount);
            var entities = landmarkMapper.selectList(wrapper);
            if (entities.isEmpty()) {
                return;
            }
            Set<ZSetOperations.TypedTuple<String>> tuples = entities.stream()
                    .map(e -> ZSetOperations.TypedTuple.of(
                            e.getId().toString(),
                            e.getCheckInCount() != null ? e.getCheckInCount().doubleValue() : 0.0))
                    .collect(Collectors.toSet());
            redisTemplate.opsForZSet().add(HOT_RANKING_KEY, tuples);
            log.info("[同步热门排行榜] 完成, 共同步 {} 个地标", entities.size());
        } catch (Exception e) {
            log.error("[同步热门排行榜] 失败", e);
        }
    }
}
