package com.park.sales.aspect;

import com.park.sales.entity.SysOperLog;
import com.park.sales.mapper.SysOperLogMapper;
import com.park.sales.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * 操作日志切面：自动记录管理端的增删改（POST/PUT/DELETE）操作。
 * 仅记录写操作，避免日志噪音；登录与日志查询本身不记录。
 */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    private static final Map<String, String> MODULE_NAMES = Map.ofEntries(
            Map.entry("FactoryUnitController", "房源管理"),
            Map.entry("CustomerController", "客户管理"),
            Map.entry("SubscriptionController", "认购管理"),
            Map.entry("ContractController", "合同管理"),
            Map.entry("ApprovalController", "审批管理"),
            Map.entry("PaymentController", "回款管理"),
            Map.entry("ConfigController", "参数配置"),
            Map.entry("SystemUserController", "用户管理"),
            Map.entry("SystemRoleController", "角色管理"),
            Map.entry("SystemDeptController", "部门管理"),
            Map.entry("SystemMenuController", "菜单管理"),
            Map.entry("ProjectController", "项目管理"));

    private final SysOperLogMapper operLogMapper;

    public OperLogAspect(SysOperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    @Pointcut("execution(* com.park.sales.controller..*(..)) "
            + "&& !execution(* com.park.sales.controller.SystemOperLogController.*(..)) "
            + "&& !execution(* com.park.sales.controller.OaCallbackController.*(..)) "
            + "&& !execution(* com.park.sales.controller.AuthController.*(..))")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        String httpMethod = request == null ? "" : request.getMethod();

        boolean shouldLog = "POST".equals(httpMethod) || "PUT".equals(httpMethod) || "DELETE".equals(httpMethod);

        long start = System.currentTimeMillis();
        Object result;
        String errorMsg = null;
        int status = 1;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable ex) {
            status = 0;
            errorMsg = ex.getMessage();
            throw ex;
        } finally {
            if (shouldLog) {
                try {
                    saveLog(pjp, request, httpMethod, status, errorMsg, System.currentTimeMillis() - start);
                } catch (Exception e) {
                    log.warn("记录操作日志失败: {}", e.getMessage());
                }
            }
        }
    }

    private void saveLog(ProceedingJoinPoint pjp, HttpServletRequest request, String httpMethod,
                         int status, String errorMsg, long cost) {
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();

        SysOperLog opLog = new SysOperLog();
        opLog.setModule(MODULE_NAMES.getOrDefault(className, className));
        opLog.setOperType(mapType(httpMethod));
        opLog.setTitle(MODULE_NAMES.getOrDefault(className, className) + "-" + mapType(httpMethod));
        opLog.setMethod(className + "." + methodName + "()");
        opLog.setRequestMethod(httpMethod);
        opLog.setRequestUri(request == null ? "" : request.getRequestURI());
        opLog.setOperName(currentUsername());
        opLog.setOperParam(truncate(safeArgs(pjp.getArgs())));
        opLog.setStatus(status);
        opLog.setErrorMsg(truncate(errorMsg));
        opLog.setCostTime(cost);
        opLog.setOperTime(java.time.LocalDateTime.now());
        operLogMapper.insert(opLog);
    }

    private String mapType(String httpMethod) {
        return switch (httpMethod) {
            case "POST" -> "CREATE";
            case "PUT" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "OTHER";
        };
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUsername();
        }
        return "匿名";
    }

    private String safeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            String name = arg.getClass().getSimpleName();
            if (name.contains("Request") || name.contains("Response") || name.contains("Session")) {
                continue;
            }
            sb.append(arg).append(" ");
        }
        return sb.toString().trim();
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
