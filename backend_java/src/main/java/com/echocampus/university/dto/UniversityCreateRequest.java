package com.echocampus.university.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UniversityCreateRequest {

    @Schema(description = "大学名称", example = "华南理工大学")
    @NotBlank(message = "大学名称不能为空")
    private String name;
}
