package com.campus.common.log;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.campus.common.core.domain.Result;
import com.campus.common.security.LoginUser;
import com.campus.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面
 *
 * @author campus-mart
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final ApplicationEventPublisher publisher;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                record(point, operLog, result, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
        }
    }

    private void record(ProceedingJoinPoint point, OperLog operLog, Object result, Throwable error, long cost) {
        OperLogEvent event = new OperLogEvent();
        event.setTitle(operLog.title());
        event.setBusinessType(String.valueOf(operLog.businessType()));

        MethodSignature sig = (MethodSignature) point.getSignature();
        event.setMethod(sig.getDeclaringTypeName() + "." + sig.getName() + "()");
        event.setOperTime(LocalDateTime.now());
        event.setCostTime(cost);

        HttpServletRequest request = currentRequest();
        if (request != null) {
            event.setRequestMethod(request.getMethod());
            event.setOperUrl(request.getRequestURI());
            event.setOperIp(clientIp(request));
            if (operLog.saveRequestData()) {
                event.setOperParam(truncate(buildParams(point.getArgs()), 2000));
            }
        }

        LoginUser user = SecurityUtils.get();
        if (user != null) {
            event.setOperName(user.getUsername());
            event.setDeptName(user.getDeptName());
            event.setOperatorType("1");
        } else {
            event.setOperatorType("0");
        }

        if (error != null) {
            event.setStatus("1");
            event.setErrorMsg(truncate(error.getMessage(), 2000));
        } else {
            event.setStatus("0");
            if (operLog.saveResponseData()) {
                event.setJsonResult(truncate(toJson(result), 2000));
            }
        }
        publisher.publishEvent(event);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    /** 取真实客户端 IP（网关会透传 X-Forwarded-For） */
    public static String clientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String h : headers) {
            String v = request.getHeader(h);
            if (StrUtil.isNotBlank(v) && !"unknown".equalsIgnoreCase(v)) {
                return v.split(",")[0].trim();
            }
        }
        String ip = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        String json = Arrays.stream(args)
                .map(a -> {
                    if (a == null) {
                        return "null";
                    }
                    if (a instanceof MultipartFile || a instanceof MultipartFile[]) {
                        return "\"<file>\"";
                    }
                    if (a instanceof HttpServletRequest || a instanceof jakarta.servlet.http.HttpServletResponse) {
                        return "\"<servlet>\"";
                    }
                    try {
                        return JSONUtil.toJsonStr(a);
                    } catch (Exception e) {
                        return "\"" + a.getClass().getSimpleName() + "\"";
                    }
                })
                .collect(Collectors.joining(","));
        // 脱敏：password / oldPassword / newPassword
        return json.replaceAll("(\"(?:password|oldPassword|newPassword|confirmPassword)\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
    }

    private String toJson(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Result<?> r) {
            return JSONUtil.toJsonStr(r);
        }
        try {
            return JSONUtil.toJsonStr(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
