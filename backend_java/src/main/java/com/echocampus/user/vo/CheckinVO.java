package com.echocampus.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinVO {

    private UUID id;

    private UUID landmarkId;

    private String landmarkName;

    private LocalDateTime createdAt;
}
