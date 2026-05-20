package com.echocampus.entity;

public class FloorEntity {
    private Integer floorNumber;
    private String floorName;
    private String tags; // JSON text

    public Integer getFloorNumber() { return floorNumber; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }
    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}
