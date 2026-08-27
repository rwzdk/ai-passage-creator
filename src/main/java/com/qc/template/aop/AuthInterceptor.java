package com.qc.template.aop;

import com.qc.template.annotation.AuthCheck;
import com.qc.template.exception.BusinessException;
import com.qc.template.exception.ErrorCode;
import com.qc.template.model.entity.User;
import com.qc.template.model.enums.UserRoleEnum;
import com.qc.template.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 鏉冮檺鏍￠獙 AOP
 *
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 鎵ц鎷︽埅
     *
     * @param joinPoint 鍒囧叆鐐?
     * @param authCheck 鏉冮檺鏍￠獙娉ㄨВ
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 涓嶉渶瑕佹潈闄愶紝鐩存帴鏀捐
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        // 浠ヤ笅鐨勪唬鐮侊細蹇呴』鏈夎繖涓潈闄愭墠鑳介€氳繃
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 娌℃湁鏉冮檺锛岀洿鎺ユ嫆缁?
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 瑕佹眰蹇呴』鏈夌鐞嗗憳鏉冮檺锛屼絾褰撳墠鐧诲綍鐢ㄦ埛娌℃湁
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 閫氳繃鏉冮檺鏍￠獙锛屾斁琛?
        return joinPoint.proceed();
    }
}
