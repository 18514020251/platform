package com.xcvk.platform.log.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.log.starter.aspect.AccessLogAspect;
import com.xcvk.platform.log.starter.properties.AccessLogProperties;
import com.xcvk.platform.log.starter.repository.AccessLogRepository;
import com.xcvk.platform.log.starter.repository.MongoAccessLogRepository;
import com.xcvk.platform.log.starter.support.AccessLogPublisher;
import com.xcvk.platform.log.starter.support.AccessLogSanitizer;
import com.xcvk.platform.log.starter.support.AccessLogUserResolver;
import com.xcvk.platform.log.starter.support.MdcTaskDecorator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 访问日志自动装配。
 *
 * <p>只在 Servlet Web 环境下启用访问日志切面，并通过配置开关控制是否生效。
 * MongoDB 写入采用条件装配，业务服务只要引入 starter 并配置 MongoDB URI 即可自动启用异步落库。</p>
 *
 * <p>注意：这里不再使用 {@code @ConditionalOnBean(MongoTemplate.class)} 判断 MongoTemplate，
 * 避免 starter 自动配置顺序导致 MongoTemplate 已经存在但条件判断未命中的问题。</p>
 *
 * @author Programmer
 * @version 1.3
 * @date 2026-05-11
 */
@AutoConfiguration(after = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@EnableConfigurationProperties(AccessLogProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "platform.access-log", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class LogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccessLogSanitizer accessLogSanitizer(ObjectMapper objectMapper,
                                                 AccessLogProperties properties) {
        return new AccessLogSanitizer(objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessLogUserResolver accessLogUserResolver() {
        return new AccessLogUserResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessLogAspect accessLogAspect(ObjectMapper objectMapper,
                                           AccessLogSanitizer sanitizer,
                                           AccessLogProperties properties,
                                           ObjectProvider<AccessLogPublisher> accessLogPublisherProvider,
                                           AccessLogUserResolver userResolver) {
        return new AccessLogAspect(objectMapper, sanitizer, properties, accessLogPublisherProvider, userResolver);
    }

    @Bean("accessLogTaskExecutor")
    @ConditionalOnMissingBean(name = "accessLogTaskExecutor")
    @ConditionalOnProperty(
            prefix = "platform.access-log",
            name = "mongo-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public ThreadPoolTaskExecutor accessLogTaskExecutor(AccessLogProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(properties.getAsyncThreadNamePrefix());
        executor.setCorePoolSize(properties.getAsyncCorePoolSize());
        executor.setMaxPoolSize(properties.getAsyncMaxPoolSize());
        executor.setQueueCapacity(properties.getAsyncQueueCapacity());
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(properties.getAsyncAwaitTerminationSeconds());
        executor.initialize();

        log.info(
                "AccessLog线程池初始化完成, corePoolSize={}, maxPoolSize={}, queueCapacity={}, threadNamePrefix={}",
                properties.getAsyncCorePoolSize(),
                properties.getAsyncMaxPoolSize(),
                properties.getAsyncQueueCapacity(),
                properties.getAsyncThreadNamePrefix()
        );

        return executor;
    }

    @Bean
    @ConditionalOnMissingBean(AccessLogRepository.class)
    @ConditionalOnProperty(
            prefix = "platform.access-log",
            name = "mongo-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public AccessLogRepository mongoAccessLogRepository(ObjectProvider<MongoTemplate> mongoTemplateProvider,
                                                        AccessLogProperties properties) {
        MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();

        if (mongoTemplate == null) {
            log.warn(
                    "跳过MongoDB访问日志仓库初始化, 原因=未找到MongoTemplate, collection={}",
                    properties.getCollectionName()
            );

            return record -> log.debug(
                    "跳过保存访问日志, 原因=MongoTemplate不存在, traceId={}, service={}, url={}",
                    record.traceId(),
                    record.serviceName(),
                    record.url()
            );
        }

        log.info("MongoDB访问日志仓库初始化完成, 集合={}", properties.getCollectionName());
        return new MongoAccessLogRepository(mongoTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "platform.access-log",
            name = "mongo-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public AccessLogPublisher accessLogPublisher(AccessLogRepository repository,
                                                 @Qualifier("accessLogTaskExecutor")
                                                 ThreadPoolTaskExecutor accessLogTaskExecutor) {
        log.info("AccessLogPublisher初始化完成");
        return new AccessLogPublisher(repository, accessLogTaskExecutor);
    }

    @Bean
    public ApplicationRunner accessLogStartupChecker(AccessLogProperties properties,
                                                     ObjectProvider<MongoTemplate> mongoTemplateProvider,
                                                     ObjectProvider<AccessLogRepository> repositoryProvider,
                                                     ObjectProvider<AccessLogPublisher> publisherProvider) {
        return args -> {
            MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
            AccessLogRepository repository = repositoryProvider.getIfAvailable();
            AccessLogPublisher publisher = publisherProvider.getIfAvailable();

            log.info(
                    "AccessLog启动检查: enabled={}, mongoEnabled={}, MongoTemplate={}, AccessLogRepository={}, AccessLogPublisher={}, collection={}",
                    properties.isEnabled(),
                    properties.isMongoEnabled(),
                    mongoTemplate != null,
                    repository != null,
                    publisher != null,
                    properties.getCollectionName()
            );
        };
    }
}