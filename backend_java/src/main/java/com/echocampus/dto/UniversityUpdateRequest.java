package com.echocampus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UniversityUpdateRequest {

    @Schema(description = "大学名称", example = "华南理工大学")
    private String name;
}
