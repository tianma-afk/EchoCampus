package com.echocampus.algorithm.controller;

import com.echocampus.algorithm.dto.CallbackRequest;
import com.echocampus.algorithm.service.AlgorithmAdminService;
import com.echocampus.shared.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/callback")
@Tag(name = "内部回调", description = "向量化任务回调")
public class CallbackAdminController {

    private final AlgorithmAdminService algorithmAdminService;

    public CallbackAdminController(AlgorithmAdminService algorithmAdminService) {
        this.algorithmAdminService = algorithmAdminService;
    }

    @PutMapping("/vectorize/{taskId}")
    @Operation(summary = "向量化任务回调", description = "向量化任务回调")
    public Result<Void> vectorizeCallBack(@PathVariable UUID taskId,
                                          @Valid @RequestBody CallbackRequest request) {
        algorithmAdminService.updateTaskStatus(taskId, request);
        return Result.success(null);
    }
}
