package com.echocampus.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

@Data
@TableName("campus")
public class CampusEntity {

    @TableId
    private UUID id;

    private String name;

    private UUID universityId;
}
