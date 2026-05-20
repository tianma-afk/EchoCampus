package com.echocampus.entity;

import java.util.List;

public class LandmarkDetailEntity {
    private String id;
    private String name;
    private Integer score;
    private Integer checkInCount;
    private String tags;
    private String openTime;
    private String categoryName;
    private String builtTime;
    private String description;
    private String locationDescription;
    private String campusName;
    private String universityName;
    private List<FloorEntity> floors;
    private List<String> images;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getCheckInCount() { return checkInCount; }
    public void setCheckInCount(Integer checkInCount) { this.checkInCount = checkInCount; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getBuiltTime() { return builtTime; }
    public void setBuiltTime(String builtTime) { this.builtTime = builtTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocationDescription() { return locationDescription; }
    public void setLocationDescription(String locationDescription) { this.locationDescription = locationDescription; }
    public String getCampusName() { return campusName; }
    public void setCampusName(String campusName) { this.campusName = campusName; }
    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }
    public List<FloorEntity> getFloors() { return floors; }
    public void setFloors(List<FloorEntity> floors) { this.floors = floors; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
}
