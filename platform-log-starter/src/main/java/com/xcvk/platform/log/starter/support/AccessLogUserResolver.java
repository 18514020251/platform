package com.xcvk.platform.log.starter.support;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * 当前登录用户解析器。
 *
 * <p>log-starter 不直接依赖 sa-token-starter，避免 starter 之间产生强耦合。
 * 当业务服务已经引入 Sa-Token 时，通过反射尽力读取当前 loginId。</p>
 *
 * <p>反射 Method 会在首次调用时缓存，避免每次接口访问都执行 Class.forName 和方法查找。</p>
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-11
 */
@Slf4j
public class AccessLogUserResolver {

    private static final String STP_UTIL_CLASS_NAME = "cn.dev33.satoken.stp.StpUtil";

    private volatile boolean initialized;

    private volatile boolean saTokenAvailable;

    private Method isLoginMethod;

    private Method getLoginIdDefaultNullMethod;

    /**
     * 获取当前登录用户 ID。
     *
     * @return 当前登录用户 ID，未登录或读取失败时返回 null
     */
    public String resolveUserId() {
        initMethodsIfNecessary();
        if (!saTokenAvailable) {
            return null;
        }

        try {
            Object isLogin = isLoginMethod.invoke(null);
            if (!(isLogin instanceof Boolean login) || !login) {
                return null;
            }

            Object loginId = getLoginIdDefaultNullMethod.invoke(null);
            return loginId == null ? null : String.valueOf(loginId);
        } catch (Exception ex) {
            log.debug("resolve current login id failed: {}", ex.getMessage());
            return null;
        }
    }

    private void initMethodsIfNecessary() {
        if (initialized) {
            return;
        }

        synchronized (this) {
            if (initialized) {
                return;
            }

            try {
                Class<?> stpUtilClass = Class.forName(STP_UTIL_CLASS_NAME);
                this.isLoginMethod = stpUtilClass.getMethod("isLogin");
                this.getLoginIdDefaultNullMethod = stpUtilClass.getMethod("getLoginIdDefaultNull");
                this.saTokenAvailable = true;
            } catch (ClassNotFoundException ex) {
                this.saTokenAvailable = false;
                log.debug("sa-token not found, skip access log user resolving");
            } catch (Exception ex) {
                this.saTokenAvailable = false;
                log.debug("init sa-token reflection methods failed: {}", ex.getMessage());
            } finally {
                this.initialized = true;
            }
        }
    }
}
