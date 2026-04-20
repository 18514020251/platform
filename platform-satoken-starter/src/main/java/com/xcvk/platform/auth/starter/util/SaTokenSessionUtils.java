package com.xcvk.platform.auth.starter.util;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.xcvk.platform.auth.starter.constant.SaTokenSessionConstants;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * Sa-Token Session 工具类
 *
 * <p>负责统一写入和读取当前登录人的轻量身份信息，
 * 供后续注解鉴权和跨服务身份识别使用。</p>
 *
 * <p>当前阶段仅维护最小必要字段：
 * 用户名、角色编码列表。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public class SaTokenSessionUtils {

    /**
     * 写入当前登录人的基础身份信息
     *
     * <p>这里不重复写入 userId，因为 Sa-Token 的 loginId 本身已经承担当前用户标识。</p>
     *
     * @param username 用户名
     * @param roleCodes 角色编码列表
     */
    public void storeLoginIdentity(String username, List<String> roleCodes) {
        SaSession session = StpUtil.getSession();
        session.set(SaTokenSessionConstants.USERNAME, username);
        session.set(SaTokenSessionConstants.ROLE_CODES, roleCodes == null ? List.of() : List.copyOf(roleCodes));
    }

    /**
     * 根据 loginId 读取角色编码列表
     *
     * @param loginId 登录ID
     * @return 角色编码列表
     */
    public List<String> getRoleCodesByLoginId(Object loginId) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        Object value = session.get(SaTokenSessionConstants.ROLE_CODES);
        return toStringList(value);
    }

    /**
     * 根据 loginId 读取用户名
     *
     * @param loginId 登录ID
     * @return 用户名，不存在时返回 null
     */
    public String getUsernameByLoginId(Object loginId) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        Object value = session.get(SaTokenSessionConstants.USERNAME);
        return value == null ? null : String.valueOf(value);
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }

        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(String::valueOf)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
        }

        String singleValue = String.valueOf(value);
        return StringUtils.hasText(singleValue) ? List.of(singleValue) : List.of();
    }
}