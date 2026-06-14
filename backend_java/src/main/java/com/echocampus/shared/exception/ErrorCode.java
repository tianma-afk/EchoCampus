package com.echocampus.shared.exception;

public enum ErrorCode {
    // ===== 通用 (0/G) =====
    SUCCESS("00000", "成功"),
    SYSTEM_ERROR("00001", "系统繁忙，请稍后再试"),

    VALIDATION_ERROR("G0001", "请求参数错误"),
    NOT_FOUND("G0002", "资源不存在"),
    CONFLICT("G0003", "资源冲突"),

    // ===== 管理员模块 (A) =====
    AUTH_TOKEN_INVALID("A0001", "未登录或登录已过期"),
    AUTH_PERMISSION_DENIED("A0002", "权限不足"),
    ADMIN_NOT_FOUND("A0003", "管理员不存在"),
    ADMIN_LOGIN_ERROR("A0004", "用户名或邮箱或密码错误"),
    

    // ===== 用户模块 (U) =====
    USER_NOT_FOUND("U1001", "用户不存在"),
    USER_ALREADY_EXISTS("U1002", "用户已存在"),
    USER_PASSWORD_ERROR("U1003", "密码错误"),
    USER_NOT_LOGIN("U1004", "未登录或登录已过期"),
    EMAIL_FORMAT_ERROR("U1005", "邮箱格式错误"),
    EMAIL_SEND_FREQUENTLY("U1006", "发送频率过高，请稍后再试"),
    EMAIL_SEND_THRESHOLD_EXCEEDED("U1007", "发送次数已达上限"),

    EMAIL_VERIFICATION_ERROR("U1009", "验证码错误"),
    EMAIL_VERIFICATION_EXPIRED("U1010", "验证码已过期"),
    NICKNAME_IS_NULL("U1011", "昵称不能为空"),
    NICKNAME_LENGTH_ERROR("U1012", "昵称长度不符合要求"),
    EMAIL_TRY_TOO_MANY_TIMES("U1013", "验证码尝试次数过多"),
    NICKNAME_ILLEGAL_CHARACTERS("U1014", "昵称包含非法字符"),
    NICKNAME_CHANGE_LIMIT("U1015", "昵称修改次数已达上限"),
    CHECKIN_ALREADY("U1016", "今日已打卡"),

    // ===== 地标/校区/大学模块 (L) =====
    LANDMARK_NOT_FOUND("L0001", "地标不存在"),
    CAMPUS_NOT_FOUND("L0002", "校区不存在"),
    UNIVERSITY_NOT_FOUND("L0003", "大学不存在"),
    CAMPUS_NAME_CONFLICT("L0004", "校区名称已存在"),
    UNIVERSITY_NAME_CONFLICT("L0005", "大学名称已存在"),
    IMAGE_NOT_FOUND("L0006", "图片不存在"),

    // ===== 反馈模块 (F) =====
    FEEDBACK_SUBMIT_TOO_FREQUENT("F0001", "提交过于频繁，请5分钟后再试"),

    // ===== 技术异常 (T) =====
    ALGORITHM_SERVICE_ERROR("T0001", "算法服务异常"),
    FILE_STORAGE_ERROR("T0002", "文件存储服务异常"),
    EMAIL_SERVICE_ERROR("T0003", "邮件服务异常");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}
