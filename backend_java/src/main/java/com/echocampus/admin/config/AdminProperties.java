package com.echocampus.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "super-admin")
@Data
public class AdminProperties {
    private String username;
    private String password; //刚刚读进来还是明文
    private String email;
}