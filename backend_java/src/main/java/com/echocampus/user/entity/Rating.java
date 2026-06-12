package com.echocampus.user.entity;

import java.util.UUID;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@TableName("rating")
public class Rating {
    @TableId
    private UUID id; //主键

    private UUID user_id;//用户主键
    private UUID landmark_uuid;//地标主键
    private Double rating;//评分
}
