package com.echocampus.algorithm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.algorithm.client.AlgorithmClient;
import com.echocampus.algorithm.dto.CallbackRequest;
import com.echocampus.campus.entity.CampusEntity;
import com.echocampus.landmark.entity.ImageEntity;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.algorithm.entity.TaskEntity;
import com.echocampus.university.entity.UniversityEntity;
import com.echocampus.shared.enums.TaskStatusEnum;
import com.echocampus.shared.enums.TaskTypeEnum;
import com.echocampus.campus.mapper.CampusMapper;
import com.echocampus.landmark.mapper.ImageMapper;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.algorithm.mapper.TaskMapper;
import com.echocampus.university.mapper.UniversityMapper;
import com.echocampus.algorithm.service.AlgorithmAdminService;
import com.echocampus.shared.annotation.TimedTask;
import com.echocampus.shared.util.CallBackUrlBuilder;
import com.echocampus.shared.util.ImageUrlBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AlgorithmAdminServiceImpl implements AlgorithmAdminService {

    private final AlgorithmClient algorithmClient;
    private final CallBackUrlBuilder callBackUrlBuilder;
    private final ImageUrlBuilder imageUrlBuilder;
    private final ImageMapper imageMapper;
    private final LandmarkMapper landmarkMapper;
    private final CampusMapper campusMapper;
    private final UniversityMapper universityMapper;
    private final TaskMapper taskMapper;

    public AlgorithmAdminServiceImpl(AlgorithmClient algorithmClient, CallBackUrlBuilder callBackUrlBuilder,
                                     ImageUrlBuilder imageUrlBuilder, ImageMapper imageMapper,
                                     LandmarkMapper landmarkMapper, CampusMapper campusMapper,
                                     UniversityMapper universityMapper, TaskMapper taskMapper) {
        this.algorithmClient = algorithmClient;
        this.callBackUrlBuilder = callBackUrlBuilder;
        this.imageUrlBuilder = imageUrlBuilder;
        this.imageMapper = imageMapper;
        this.landmarkMapper = landmarkMapper;
        this.campusMapper = campusMapper;
        this.universityMapper = universityMapper;
        this.taskMapper = taskMapper;
    }

    @Override
    @TimedTask
    public UUID createVectorTaskForAllImages() {
        List<ImageEntity> images = getUnvectoredImages();
        if (images.isEmpty()) {
            return null;
        }

        // 设置缓存，防止地标相同的情况下重复查询学院大学
        Map<UUID, LandmarkEntity> landmarkCache = new HashMap<>();
        Map<UUID, CampusEntity> campusCache = new HashMap<>();
        Map<UUID, UniversityEntity> universityCache = new HashMap<>();

        //获取未向量化的图片
        Map<UUID, String> idsAndUrls = new HashMap<>();
        for (ImageEntity image : images) {
            UUID landmarkId = image.getLandmarkId();
            LandmarkEntity landmark = landmarkCache.computeIfAbsent(landmarkId,
                    id -> landmarkMapper.selectById(id));

            UUID campusId = landmark.getCampusId();
            CampusEntity campus = campusCache.computeIfAbsent(campusId,
                    id -> campusMapper.selectById(id));

            UUID universityId = campus.getUniversityId();
            UniversityEntity university = universityCache.computeIfAbsent(universityId,
                    id -> universityMapper.selectById(id));

            String url = imageUrlBuilder.buildUrl(
                    university.getId(), campus.getId(), landmark.getId(),
                    image.getId(), image.getFileExt());
            idsAndUrls.put(image.getId(), url);
        }

        //创建任务
        TaskEntity task = new TaskEntity();
        task.setId(UUID.randomUUID());
        task.setTaskType(TaskTypeEnum.VECTORIZE.getValue());
        task.setTaskStatus(TaskStatusEnum.READY.getValue());
        taskMapper.insert(task);
        UUID taskId = task.getId();

        //构造回调url，发送任务
        String callbackUrl = callBackUrlBuilder.build(taskId.toString(), TaskTypeEnum.VECTORIZE);
        String algTaskId = algorithmClient.submitInsertTask(idsAndUrls, callbackUrl);
        task.setAlgTaskId(algTaskId);
        taskMapper.updateById(task);

        return taskId;
    }

    @Override
    @TimedTask
    public void updateTaskStatus(UUID taskId, CallbackRequest request) {
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setTaskStatus(request.getResult());
        taskMapper.updateById(task);

        if (TaskStatusEnum.SUCCESS.getValue().equals(request.getResult())) {
            LambdaUpdateWrapper<ImageEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(ImageEntity::getIsVectorized, true)
                    .eq(ImageEntity::getIsVectorized, false);
            imageMapper.update(updateWrapper);
        }
    }

    @Override
    public TaskEntity getTaskStatus(UUID taskId) {
        return taskMapper.selectById(taskId);
    }

    @Override
    public Page<TaskEntity> listTasks(int page, int pageSize) {
        LambdaQueryWrapper<TaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(TaskEntity::getCreatedAt);
        return taskMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    private List<ImageEntity> getUnvectoredImages() {
        LambdaQueryWrapper<ImageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImageEntity::getIsVectorized, false);
        return imageMapper.selectList(wrapper);
    }
}
