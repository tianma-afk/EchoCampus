package com.echocampus.dto;

import lombok.Data;

@Data
public class LandmarkCreateRequest {
    private String name;
    private String location;
}
