package com.echocampus.landmark.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.echocampus.shared.handler.JsonbTypeHandler;
import com.echocampus.shared.handler.UuidListJsonbTypeHandler;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@TableName(value = "landmark", autoResultMap = true)
public class LandmarkEntity {

    @TableId
    private UUID id;

    private String name;

    private BigDecimal rating;

    private Integer checkInCount;

    private String openTime;

    private UUID categoryId;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private List<String> tags;

    @TableField(typeHandler = UuidListJsonbTypeHandler.class)
    private List<UUID> imgs;

    private UUID coverImageId;

    private String buildYear;

    private String openTimeDetail;

    private String floors;

    private String location;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String description;

    private UUID campusId;

    private Integer totalFloors;

    private BigDecimal recommendRate;

    private Integer favoriteCount;

    // Convert JSONB-deserialized String elements to UUID, avoiding ClassCastException in enhanced for-loops.
    public List<UUID> getImgs() {
        if (this.imgs == null) return null;
        return this.imgs.stream()
                .map(o -> o instanceof UUID ? (UUID) o : UUID.fromString(o.toString()))
                .collect(Collectors.toList());
    }
}
