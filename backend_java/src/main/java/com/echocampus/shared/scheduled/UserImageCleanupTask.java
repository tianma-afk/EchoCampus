package com.echocampus.shared.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.shared.util.MinioUtil;
import com.echocampus.user.entity.UserImage;
import com.echocampus.user.mapper.UserImageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class UserImageCleanupTask {

    private static final String UPLOAD_BUCKET = "campus";
    private static final int RETENTION_DAYS = 7;

    private final UserImageMapper userImageMapper;
    private final MinioUtil minioUtil;

    public UserImageCleanupTask(UserImageMapper userImageMapper, MinioUtil minioUtil) {
        this.userImageMapper = userImageMapper;
        this.minioUtil = minioUtil;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredUserImages() {
        OffsetDateTime deadline = OffsetDateTime.now().minusDays(RETENTION_DAYS);
        List<UserImage> expired = userImageMapper.selectList(
                new LambdaQueryWrapper<UserImage>()
                        .lt(UserImage::getCreatedAt, deadline));

        if (expired.isEmpty()) {
            log.debug("[用户图片清理] 无过期图片");
            return;
        }

        log.info("[用户图片清理] 发现 {} 条过期图片，开始清理", expired.size());

        for (UserImage img : expired) {
            try {
                minioUtil.removeObject(UPLOAD_BUCKET, img.getObjectName());
            } catch (Exception e) {
                log.warn("[用户图片清理] 删除 MinIO 文件失败: objectName={}", img.getObjectName(), e);
            }
        }

        List<UUID> ids = expired.stream().map(UserImage::getId).toList();
        userImageMapper.delete(new LambdaQueryWrapper<UserImage>().in(UserImage::getId, ids));
        log.info("[用户图片清理] 完成: 删除 {} 条记录", ids.size());
    }
}
