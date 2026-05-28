package com.echocampus.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.type.JdbcType;

import java.util.UUID;

@Data
@TableName("category")
public class CategoryEntity {

    @TableId
    private UUID id;

    private String name;
}
