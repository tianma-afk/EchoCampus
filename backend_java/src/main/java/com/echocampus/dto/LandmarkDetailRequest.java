package com.echocampus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
public class LandmarkDetailRequest {

    @Schema(description = "地标ID")
    private UUID id;
}
