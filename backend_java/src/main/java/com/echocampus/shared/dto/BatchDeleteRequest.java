package com.echocampus.shared.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BatchDeleteRequest {

    @NotEmpty(message = "删除ID列表不能为空")
    private List<UUID> ids;
}
