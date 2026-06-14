package com.echocampus.shared.interceptor;

import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.context.AuthContext;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.util.JwtUtil;
import com.echocampus.shared.vo.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("[JWT拦截器] 缺少Authorization头 -> path={}", request.getRequestURI());
            writeError(response, ErrorCode.AUTH_TOKEN_INVALID);
            return false;
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        JwtUtil.TokenClaims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            log.warn("[JWT拦截器] 令牌已过期 -> path={}", request.getRequestURI());
            writeError(response, ErrorCode.AUTH_TOKEN_INVALID, "令牌已过期");
            return false;
        } catch (SignatureException e) {
            log.warn("[JWT拦截器] 签名无效 -> path={}", request.getRequestURI());
            writeError(response, ErrorCode.AUTH_TOKEN_INVALID);
            return false;
        } catch (MalformedJwtException | IllegalArgumentException e) {
            log.warn("[JWT拦截器] 令牌格式错误 -> path={}", request.getRequestURI());
            writeError(response, ErrorCode.AUTH_TOKEN_INVALID);
            return false;
        } catch (JwtException e) {
            log.warn("[JWT拦截器] JWT验证失败 -> path={}, reason={}", request.getRequestURI(), e.getMessage());
            writeError(response, ErrorCode.AUTH_TOKEN_INVALID);
            return false;
        }

        if (claims.role() == null) {
            log.warn("[JWT拦截器] 令牌中无角色信息 -> path={}", request.getRequestURI());
            writeError(response, ErrorCode.AUTH_TOKEN_INVALID);
            return false;
        }

        if (claims.role().getLevel() < requireRole.value().getLevel()) {
            log.warn("[JWT拦截器] 权限不足 -> path={}, required={}, actual={}",
                    request.getRequestURI(), requireRole.value(), claims.role());
            writeError(response, ErrorCode.AUTH_PERMISSION_DENIED);
            return false;
        }

        AuthContext.set(claims.userId(), claims.role());

        log.debug("[JWT拦截器] 认证通过 -> userId={}, role={}, path={}",
                claims.userId(), claims.role(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws Exception {
        writeError(response, errorCode, errorCode.getMessage());
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.failure(errorCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
