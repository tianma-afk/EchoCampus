package com.echocampus.user.entity;

import java.util.UUID;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@TableName("record")
public class RecognitionRecord {
    @TableId
    private UUID id;

    private UUID user_id;
    private UUID image_id;
}
