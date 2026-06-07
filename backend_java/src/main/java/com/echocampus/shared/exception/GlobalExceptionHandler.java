package com.echocampus.shared.exception;

import com.echocampus.shared.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理参数校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        log.info("参数校验失败 | 请求路径: {}", request.getRequestURI());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                errors.put(fe.getField(), fe.getDefaultMessage()));

        return Result.failure(ErrorCode.VALIDATION_ERROR, errors);
    }

    // 处理路径参数/请求参数校验失败
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Map<String, String>> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        log.info("参数校验失败 | 请求路径: {}", request.getRequestURI());

        Map<String, String> errors = new HashMap<>();
        e.getConstraintViolations().forEach(cv -> {
            String paramName = cv.getPropertyPath().toString();
            errors.put(paramName, cv.getMessage());
        });

        return Result.failure(ErrorCode.VALIDATION_ERROR, errors);
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.info("业务异常 | 请求路径: {} | 错误码: {} | 错误信息: {}",
                request.getRequestURI(), e.getCode(), e.getMessage());

        ErrorCode errorCode = Arrays.stream(ErrorCode.values())
                .filter(ec -> ec.getCode().equals(e.getCode()))
                .findFirst()
                .orElse(ErrorCode.SYSTEM_ERROR);

        return Result.failure(errorCode, e.getMessage());
    }

    // 处理技术异常
    @ExceptionHandler(TechnicalException.class)
    public Result<?> handleTechnicalException(TechnicalException e, HttpServletRequest request) {
        log.error("技术异常 | 请求路径: {} | 错误码: {} | 错误信息: {} ",
                request.getRequestURI(), e.getCode(), e.getMessage(), e);

        return Result.failure(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getMessage());
    }

    // 处理未捕获的异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统未知异常 | 请求路径: {} | 异常类型: {}",
                request.getRequestURI(), e.getClass().getSimpleName(), e);
        return Result.failure(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getMessage());
    }
}
