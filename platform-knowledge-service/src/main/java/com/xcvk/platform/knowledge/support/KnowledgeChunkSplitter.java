package com.xcvk.platform.knowledge.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识文档切片器
 *
 * <p>用于将知识文档正文拆分为多个较小的文本片段，
 * 为后续 embedding 向量化和语义检索做准备。</p>
 *
 * <p>当前阶段采用简单、可解释的字符级切片策略：</p>
 * <ul>
 *     <li>优先按段落切分</li>
 *     <li>过短段落合并，避免语义过碎</li>
 *     <li>过长段落按固定窗口切分，并保留少量重叠上下文</li>
 * </ul>
 *
 * <p>注意：当前 tokenCount 可先使用字符数近似，
 * 后续接入具体 embedding 模型后，可再替换为真实 tokenizer 统计。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-26
 */
@Component
public class KnowledgeChunkSplitter {

    /**
     * 单个 chunk 最大字符数
     */
    private static final int MAX_CHUNK_LENGTH = 800;

    /**
     * 相邻 chunk 重叠字符数
     */
    private static final int OVERLAP_LENGTH = 100;

    /**
     * 最小段落字符数，低于该长度时会尝试与后续段落合并
     */
    private static final int MIN_PARAGRAPH_LENGTH = 50;

    /**
     * 将知识文档正文切分为多个 chunk 文本。
     *
     * @param content 知识文档正文
     * @return chunk 文本列表
     */
    public List<String> split(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }

        String normalizedContent = normalize(content);
        List<String> paragraphs = splitByParagraph(normalizedContent);
        List<String> mergedParagraphs = mergeShortParagraphs(paragraphs);

        List<String> result = new ArrayList<>();
        for (String paragraph : mergedParagraphs) {
            if (StringUtils.hasText(paragraph)) {
                String trimmedParagraph = paragraph.trim();

                if (trimmedParagraph.length() <= MAX_CHUNK_LENGTH) {
                    result.add(trimmedParagraph);
                } else {
                    result.addAll(splitLongText(trimmedParagraph));
                }
            }
        }

        return result;
    }

    /**
     * 标准化文本换行。
     *
     * @param content 原始文本
     * @return 标准化后的文本
     */
    private String normalize(String content) {
        return content.replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    /**
     * 按段落切分文本。
     *
     * <p>使用空行作为段落边界，兼容多个空行或空白字符。</p>
     *
     * @param content 文档正文
     * @return 段落列表
     */
    private List<String> splitByParagraph(String content) {
        String[] paragraphs = content.split("\\n\\s*\\n");

        List<String> result = new ArrayList<>();
        for (String paragraph : paragraphs) {
            if (StringUtils.hasText(paragraph)) {
                result.add(paragraph.trim());
            }
        }

        return result;
    }

    /**
     * 合并过短段落。
     *
     * <p>过短的文本片段通常缺少完整语义，
     * 合并后更适合作为 embedding 输入。</p>
     *
     * @param paragraphs 原始段落列表
     * @return 合并后的段落列表
     */
    private List<String> mergeShortParagraphs(List<String> paragraphs) {
        if (paragraphs.isEmpty()) {
            return List.of();
        }

        List<String> mergedParagraphs = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (StringUtils.hasText(paragraph)) {
                String trimmedParagraph = paragraph.trim();

                if (current.isEmpty()) {
                    current.append(trimmedParagraph);
                } else if (current.length() < MIN_PARAGRAPH_LENGTH) {
                    current.append("\n\n").append(trimmedParagraph);
                } else {
                    mergedParagraphs.add(current.toString());
                    current = new StringBuilder(trimmedParagraph);
                }
            }
        }

        if (!current.isEmpty()) {
            mergedParagraphs.add(current.toString());
        }

        return mergedParagraphs;
    }

    /**
     * 将超长文本按滑动窗口切分。
     *
     * <p>相邻 chunk 保留一定重叠字符，避免语义在边界处被截断。</p>
     *
     * @param text 超长文本
     * @return chunk 文本列表
     */
    private List<String> splitLongText(String text) {
        List<String> chunks = new ArrayList<>();

        int step = MAX_CHUNK_LENGTH - OVERLAP_LENGTH;
        if (step <= 0) {
            step = MAX_CHUNK_LENGTH;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, text.length());

            String chunk = text.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }

            if (end >= text.length()) {
                break;
            }

            start += step;
        }

        return chunks;
    }
}