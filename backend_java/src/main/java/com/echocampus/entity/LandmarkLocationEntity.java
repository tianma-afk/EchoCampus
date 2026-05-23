package com.echocampus.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

@Data
@TableName("landmark_location")
public class LandmarkLocationEntity {

    @TableId
    private UUID id;

    private String description;
}
