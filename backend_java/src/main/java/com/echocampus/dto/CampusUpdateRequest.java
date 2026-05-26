package com.echocampus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
public class CampusUpdateRequest {

    @Schema(description = "校区名称", example = "大学城校区")
    private String name;

    @Schema(description = "所属大学ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID universityId;
}
