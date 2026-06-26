package com.echocampus.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "打卡请求")
public class CheckinRequest {

    @NotNull
    @Schema(description = "用户纬度", example = "30.123456")
    private BigDecimal latitude;

    @NotNull
    @Schema(description = "用户经度", example = "120.123456")
    private BigDecimal longitude;
}
