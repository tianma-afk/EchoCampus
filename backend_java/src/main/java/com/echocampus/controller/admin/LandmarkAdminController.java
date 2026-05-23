package com.echocampus.controller.admin;

import com.echocampus.dto.LandmarkCreateRequest;
import com.echocampus.vo.Result;
import com.echocampus.service.admin.LandmarkAdminService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/landmarks")
public class LandmarkAdminController {
        private final LandmarkAdminService landmarkAdminService;

    public LandmarkAdminController(LandmarkAdminService landmarkAdminService) {
        this.landmarkAdminService = landmarkAdminService;
    }

    @PostMapping("/")
    public Result<Long> createLandmark(@RequestBody LandmarkCreateRequest request) {
        Long id = landmarkAdminService.createLandmark(request);
        return Result.success(id);
    }

    @GetMapping("/")
    public Result<String> getLandmarkDetail() {
        return Result.success("hello world");
    }

}
