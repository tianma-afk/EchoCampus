package com.echocampus.utils;

import com.echocampus.enums.TaskTypeEnum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CallBackUrlBuilder {
    @Value("${callback.protocol}")
    private String protocol;

    @Value("${callback.host}")
    private String host;

    @Value("${callback.port}")
    private String port;

    @Value("${callback.base-path}")
    private String basePath;

    /**
     * 构建回调URL
     *
     * @param taskId 任务ID
     * @param type 回调任务类型（vectorize/search）
     * @return 完整的回调URL
     */
    public String build(String taskId, TaskTypeEnum type) {
        StringBuilder url = new StringBuilder();
        url.append(protocol).append("://").append(host).append(":").append(port)
                .append(basePath).append("/").append(type.getValue().toLowerCase()).append("/").append(taskId);
        return url.toString();
    }

}
