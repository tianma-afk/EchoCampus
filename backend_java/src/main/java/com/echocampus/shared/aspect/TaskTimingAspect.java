package com.echocampus.shared.aspect;

import com.echocampus.shared.annotation.TimedTask;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
public class TaskTimingAspect {

    private final Map<UUID, Long> startTimes = new ConcurrentHashMap<>();

    @Around("(execution(* com.echocampus.algorithm.service.impl.AlgorithmUserServiceImpl.createSearchTask(..)) || execution(* com.echocampus.algorithm.service.impl.AlgorithmAdminServiceImpl.createVectorTaskForAllImages(..))) && @annotation(timedTask)")
    public Object recordStart(ProceedingJoinPoint pjp, TimedTask timedTask) throws Throwable {
        Object result = pjp.proceed();
        if (result instanceof UUID taskId) {
            startTimes.put(taskId, System.currentTimeMillis());
        }
        return result;
    }

    @Around("(execution(* com.echocampus.algorithm.service.impl.AlgorithmUserServiceImpl.updateSearchTaskStatus(..)) || execution(* com.echocampus.algorithm.service.impl.AlgorithmAdminServiceImpl.updateTaskStatus(..))) && @annotation(timedTask)")
    public Object recordEnd(ProceedingJoinPoint pjp, TimedTask timedTask) throws Throwable {
        Object[] args = pjp.getArgs();
        if (args.length > 0 && args[0] instanceof UUID taskId) {
            Long start = startTimes.remove(taskId);
            if (start != null) {
                long elapsed = System.currentTimeMillis() - start;
                String taskType = pjp.getSignature().getName().contains("Search") ? "识别" : "向量化";
                log.info("{}任务耗时: taskId={}, duration={}ms", taskType, taskId, elapsed);
            }
        }
        return pjp.proceed();
    }
}
