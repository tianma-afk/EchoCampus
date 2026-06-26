package com.echocampus.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.user.entity.CheckinEntity;
import com.echocampus.user.mapper.CheckinMapper;
import com.echocampus.user.service.CheckinService;
import com.echocampus.user.vo.CheckinVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CheckinServiceImpl implements CheckinService {

    private static final String HOT_RANKING_KEY = "landmark:hot:ranking";
    private static final String CHECKIN_DAILY_PREFIX = "checkin:daily:";

    private final CheckinMapper checkinMapper;
    private final LandmarkMapper landmarkMapper;
    private final StringRedisTemplate redisTemplate;

    public CheckinServiceImpl(CheckinMapper checkinMapper, LandmarkMapper landmarkMapper,
                              StringRedisTemplate redisTemplate) {
        this.checkinMapper = checkinMapper;
        this.landmarkMapper = landmarkMapper;
        this.redisTemplate = redisTemplate;
    }

    private static final double MAX_CHECKIN_DISTANCE_METERS = 200;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doCheckin(UUID userId, UUID landmarkId, BigDecimal latitude, BigDecimal longitude) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new BusinessException(ErrorCode.LANDMARK_NOT_FOUND);
        }

        if (landmark.getLatitude() != null && landmark.getLongitude() != null) {
            double distance = calculateDistance(
                    latitude.doubleValue(), longitude.doubleValue(),
                    landmark.getLatitude().doubleValue(), landmark.getLongitude().doubleValue());
            if (distance > MAX_CHECKIN_DISTANCE_METERS) {
                log.warn("[打卡] 超出范围 userId={}, landmarkId={}, distance={}m", userId, landmarkId, (int) distance);
                throw new BusinessException(ErrorCode.CHECKIN_OUT_OF_RANGE);
            }
        }

        String dailyKey = CHECKIN_DAILY_PREFIX + userId + ":" + landmarkId + ":" + LocalDate.now();
        Boolean alreadyChecked = redisTemplate.opsForValue().setIfAbsent(dailyKey, "1", 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(alreadyChecked)) {
            throw new BusinessException(ErrorCode.CHECKIN_ALREADY);
        }

        CheckinEntity checkin = new CheckinEntity();
        checkin.setId(UUID.randomUUID());
        checkin.setUserId(userId);
        checkin.setLandmarkId(landmarkId);
        checkin.setCreatedAt(LocalDateTime.now());
        try {
            checkinMapper.insert(checkin);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CHECKIN_ALREADY);
        }

        landmarkMapper.update(null, new LambdaUpdateWrapper<LandmarkEntity>()
                .eq(LandmarkEntity::getId, landmarkId)
                .setSql("check_in_count = check_in_count + 1"));

        try {
            redisTemplate.opsForZSet().incrementScore(HOT_RANKING_KEY, landmarkId.toString(), 1);
        } catch (Exception e) {
            log.warn("[打卡] Redis ZSET 更新失败, landmarkId={}", landmarkId, e);
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public Page<CheckinVO> getCheckinHistory(UUID userId, int page, int size) {
        Page<CheckinEntity> entityPage = new Page<>(page, size);
        checkinMapper.selectPage(entityPage, new LambdaQueryWrapper<CheckinEntity>()
                .eq(CheckinEntity::getUserId, userId)
                .orderByDesc(CheckinEntity::getCreatedAt));

        List<CheckinEntity> records = entityPage.getRecords();
        if (records.isEmpty()) {
            Page<CheckinVO> emptyPage = new Page<>(page, size, entityPage.getTotal());
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        List<UUID> landmarkIds = records.stream()
                .map(CheckinEntity::getLandmarkId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, String> landmarkNameMap = landmarkMapper.selectBatchIds(landmarkIds).stream()
                .collect(Collectors.toMap(LandmarkEntity::getId, LandmarkEntity::getName));

        List<CheckinVO> voList = records.stream()
                .map(r -> CheckinVO.builder()
                        .id(r.getId())
                        .landmarkId(r.getLandmarkId())
                        .landmarkName(landmarkNameMap.getOrDefault(r.getLandmarkId(), "未知地标"))
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        Page<CheckinVO> voPage = new Page<>(page, size, entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}
