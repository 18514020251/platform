package com.xcvk.platform.knowledge.assembler;

import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import com.xcvk.platform.knowledge.constant.KnowledgeChunkStatusConstants;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识文档切片转换器
 *
 * <p>用于将切片器生成的 chunk 文本列表转换为可入库的 KnowledgeDocumentChunk 实体列表。</p>
 *
 * <p>职责边界：</p>
 * <ul>
 *     <li>不负责切分文本，文本切分由 KnowledgeChunkSplitter 负责</li>
 *     <li>不负责保存数据库，数据保存由 Service 负责</li>
 *     <li>不负责生成向量，embedding 后续由 AI 模块或向量化流程负责</li>
 * </ul>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-26
 */
@Component
@RequiredArgsConstructor
public class KnowledgeDocumentChunkAssembler {

    private static final String HASH_ALGORITHM = "SHA-256";

    private final SnowflakeIdGenerator idGenerator;

    /**
     * 将 chunk 文本列表转换为切片实体列表。
     *
     * @param documentId 知识文档ID
     * @param chunkTexts chunk 文本列表
     * @return 知识文档切片实体列表
     */
    public List<KnowledgeDocumentChunk> toChunks(Long documentId, List<String> chunkTexts) {
        if (documentId == null || CollectionUtils.isEmpty(chunkTexts)) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<KnowledgeDocumentChunk> chunks = new ArrayList<>();

        int chunkNo = 1;
        for (String chunkText : chunkTexts) {
            if (!StringUtils.hasText(chunkText)) {
                continue;
            }

            String normalizedChunkText = chunkText.trim();

            KnowledgeDocumentChunk chunk = new KnowledgeDocumentChunk()
                    .setId(idGenerator.nextId())
                    .setDocumentId(documentId)
                    .setChunkNo(chunkNo)
                    .setChunkText(normalizedChunkText)
                    .setChunkHash(sha256(normalizedChunkText))
                    .setTokenCount(normalizedChunkText.length())
                    .setStatus(KnowledgeChunkStatusConstants.ACTIVE)
                    .setCreatedAt(now)
                    .setUpdatedAt(now);

            chunks.add(chunk);
            chunkNo++;
        }

        return chunks;
    }

    /**
     * 计算文本 SHA-256 哈希。
     *
     * <p>当前阶段用于标识 chunk 内容指纹，后续可用于判断切片内容是否发生变化。</p>
     *
     * @param text 文本内容
     * @return SHA-256 哈希字符串
     */
    private String sha256(String text) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = messageDigest.digest(text.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256 哈希算法", ex);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}