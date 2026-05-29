package com.echocampus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class FloorCreateDTO {

    @Schema(description = "楼层编号，1=1F，-1=-1F，0=室外", example = "1")
    private Integer floorNumber;

    @Schema(description = "楼层名称", example = "1F")
    private String floorName;

    @Schema(description = "楼层标签", example = "[\"自习\", \"安静\"]")
    private List<String> tags;
}
