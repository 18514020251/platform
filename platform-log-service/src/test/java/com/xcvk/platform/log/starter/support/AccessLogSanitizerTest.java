package com.xcvk.platform.log.starter.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.log.starter.properties.AccessLogProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessLogSanitizerTest {

    private final AccessLogSanitizer sanitizer = new AccessLogSanitizer(
            new ObjectMapper(),
            new AccessLogProperties()
    );

    @Test
    @DisplayName("sanitizeArgs 遇到 null 或空数组时应该返回空列表")
    void shouldReturnEmptyListWhenArgsIsNullOrEmpty() {
        assertTrue(sanitizer.sanitizeArgs(null).isEmpty());
        assertTrue(sanitizer.sanitizeArgs(new Object[0]).isEmpty());
    }

    @Test
    @DisplayName("Map 参数中的敏感字段应该被脱敏")
    void shouldMaskSensitiveFieldsInMap() {
        Map<String, Object> arg = Map.of(
                "username", "admin",
                "password", "123456",
                "token", "abc-token",
                "authorization", "Bearer xxx"
        );

        List<Object> result = sanitizer.sanitizeArgs(new Object[]{arg});

        assertEquals(1, result.size());

        Map<?, ?> sanitizedMap = assertInstanceOf(Map.class, result.get(0));

        assertEquals("admin", sanitizedMap.get("username"));
        assertEquals("******", sanitizedMap.get("password"));
        assertEquals("******", sanitizedMap.get("token"));
        assertEquals("******", sanitizedMap.get("authorization"));
    }

    @Test
    @DisplayName("敏感字段匹配应该忽略大小写")
    void shouldMaskSensitiveFieldsIgnoreCase() {
        Map<String, Object> arg = Map.of(
                "Password", "123456",
                "ACCESS_TOKEN", "access-token",
                "Authorization", "Bearer xxx"
        );

        List<Object> result = sanitizer.sanitizeArgs(new Object[]{arg});

        Map<?, ?> sanitizedMap = assertInstanceOf(Map.class, result.get(0));

        assertEquals("******", sanitizedMap.get("Password"));
        assertEquals("******", sanitizedMap.get("ACCESS_TOKEN"));
        assertEquals("******", sanitizedMap.get("Authorization"));
    }

    @Test
    @DisplayName("嵌套 Map 和 List 中的敏感字段也应该被脱敏")
    void shouldMaskSensitiveFieldsInNestedStructure() {
        Map<String, Object> arg = Map.of(
                "user", Map.of(
                        "username", "admin",
                        "password", "123456"
                ),
                "tokens", List.of(
                        Map.of("token", "token-1"),
                        Map.of("refreshToken", "refresh-token-1")
                )
        );

        List<Object> result = sanitizer.sanitizeArgs(new Object[]{arg});

        Map<?, ?> sanitizedMap = assertInstanceOf(Map.class, result.get(0));

        Map<?, ?> user = assertInstanceOf(Map.class, sanitizedMap.get("user"));
        assertEquals("admin", user.get("username"));
        assertEquals("******", user.get("password"));

        List<?> tokens = assertInstanceOf(List.class, sanitizedMap.get("tokens"));
        Map<?, ?> firstToken = assertInstanceOf(Map.class, tokens.get(0));
        Map<?, ?> secondToken = assertInstanceOf(Map.class, tokens.get(1));

        assertEquals("******", firstToken.get("token"));
        assertEquals("******", secondToken.get("refreshToken"));
    }

    @Test
    @DisplayName("普通 Java 对象中的敏感字段应该被脱敏")
    void shouldMaskSensitiveFieldsInPojo() {
        LoginCommand command = new LoginCommand(
                "admin",
                "123456",
                "login-token"
        );

        List<Object> result = sanitizer.sanitizeArgs(new Object[]{command});

        Map<?, ?> sanitizedMap = assertInstanceOf(Map.class, result.get(0));

        assertEquals("admin", sanitizedMap.get("username"));
        assertEquals("******", sanitizedMap.get("password"));
        assertEquals("******", sanitizedMap.get("token"));
    }

    @Test
    @DisplayName("数组参数应该被转换为 List 并递归脱敏")
    void shouldSanitizeArrayArgs() {
        Object[] arrayArg = new Object[]{
                Map.of("username", "admin", "password", "123456"),
                Map.of("username", "guest", "token", "guest-token")
        };

        List<Object> result = sanitizer.sanitizeArgs(new Object[]{arrayArg});

        List<?> sanitizedArray = assertInstanceOf(List.class, result.get(0));
        assertEquals(2, sanitizedArray.size());

        Map<?, ?> first = assertInstanceOf(Map.class, sanitizedArray.get(0));
        Map<?, ?> second = assertInstanceOf(Map.class, sanitizedArray.get(1));

        assertEquals("admin", first.get("username"));
        assertEquals("******", first.get("password"));

        assertEquals("guest", second.get("username"));
        assertEquals("******", second.get("token"));
    }

    @Test
    @DisplayName("MultipartFile 参数只记录文件基础信息，不记录文件内容")
    void shouldOnlyKeepFileMetadataWhenArgIsMultipartFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello world".getBytes()
        );

        List<Object> result = sanitizer.sanitizeArgs(new Object[]{file});

        Map<?, ?> fileInfo = assertInstanceOf(Map.class, result.get(0));

        assertEquals("test.txt", fileInfo.get("fileName"));
        assertEquals(11L, fileInfo.get("size"));
        assertEquals("text/plain", fileInfo.get("contentType"));
    }

    @Test
    @DisplayName("sanitizeResult 遇到 null 时应该返回 null")
    void shouldReturnNullWhenResultIsNull() {
        assertNull(sanitizer.sanitizeResult(null));
    }

    @Test
    @DisplayName("响应结果中的敏感字段也应该被脱敏")
    void shouldMaskSensitiveFieldsInResult() {
        Map<String, Object> resultBody = Map.of(
                "code", 200,
                "data", Map.of(
                        "accessToken", "access-token-value",
                        "refreshToken", "refresh-token-value"
                )
        );

        Object result = sanitizer.sanitizeResult(resultBody);

        Map<?, ?> sanitizedResult = assertInstanceOf(Map.class, result);
        Map<?, ?> data = assertInstanceOf(Map.class, sanitizedResult.get("data"));

        assertEquals(200, sanitizedResult.get("code"));
        assertEquals("******", data.get("accessToken"));
        assertEquals("******", data.get("refreshToken"));
    }

    private record LoginCommand(
            String username,
            String password,
            String token
    ) {
    }
}