package com.echocampus.university.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

@Data
@TableName("university")
public class UniversityEntity {

    @TableId
    private UUID id;

    private String name;
}
