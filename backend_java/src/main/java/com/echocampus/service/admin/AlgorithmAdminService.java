package com.echocampus.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.dto.CallbackRequest;
import com.echocampus.entity.TaskEntity;

import java.util.UUID;

public interface AlgorithmAdminService {

    UUID createVectorTaskForAllImages();

    void updateTaskStatus(UUID taskId, CallbackRequest request);

    TaskEntity getTaskStatus(UUID taskId);

    Page<TaskEntity> listTasks(int page, int pageSize);
}
