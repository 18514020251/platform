package com.xcvk.platform.auth.starter.startup;

import cn.dev33.satoken.SaManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Sa-Token 持久层检查器
 *
 * <p>用于在应用启动后打印当前 Sa-Token 持久层实现，
 * 方便确认是否已经从默认实现切换到 Redis 实现。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
@Slf4j
public class SaTokenDaoChecker implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Object dao = SaManager.getSaTokenDao();
        log.info("""
            ======== Middleware Check (satoken-starter) ========
            SaTokenDao: {}
            ===================================================
            """, dao.getClass().getName());
    }
}