package com.echocampus.user.entity;

import java.util.UUID;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@TableName("checkin")
public class Checkin {
    @TableId
    private UUID id;

    private UUID user_id;
}
