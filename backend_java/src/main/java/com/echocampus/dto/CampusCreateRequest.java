package com.echocampus.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CampusCreateRequest {
    private String name;
    private UUID universityId;
}
