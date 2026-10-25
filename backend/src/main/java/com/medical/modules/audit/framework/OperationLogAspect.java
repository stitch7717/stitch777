package com.medical.modules.audit.framework;

import com.medical.modules.audit.entity.SysOperationLog;
import com.medical.modules.audit.service.AuditService;
import com.medical.framework.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 操作留痕：拦截所有 controller 写操作（POST/PUT/DELETE），自动记录日志
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final AuditService auditService;

    @Around("execution(* com.medical..controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        boolean isWrite = request != null
                && !"GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/api/");
        if (!isWrite) {
            return joinPoint.proceed();
        }

        long start = System.currentTimeMillis();
        Integer status = 1;
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            status = 0;
            if (e instanceof NoResourceFoundException) throw e;
            throw e;
        } finally {
            try {
                SysOperationLog entry = new SysOperationLog();
                LoginUser user = currentUser();
                if (user != null) {
                    entry.setUserId(user.getId());
                    entry.setUsername(user.getUsername());
                }
                String uri = request.getRequestURI();
                // module = /api/ 后第一段
                String[] parts = uri.substring(5).split("/");
                entry.setModule(parts.length > 0 ? parts[0] : uri);
                entry.setOperation(request.getMethod() + " " + uri);
                entry.setMethod(request.getMethod() + " " + uri);
                entry.setIp(request.getRemoteAddr());
                entry.setStatus(status);
                entry.setCostTime((int) (System.currentTimeMillis() - start));
                auditService.saveOperationLog(entry);
            } catch (Exception ignored) {
                // 日志失败不阻断业务
            }
        }
    }

    private LoginUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof LoginUser u ? u : null;
    }
}
