package com.xcvk.platform.knowledge.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeChunkSplitterTest {

    private final KnowledgeChunkSplitter splitter = new KnowledgeChunkSplitter();

    @Test
    @DisplayName("空文本或空白文本应该返回空切片列表")
    void shouldReturnEmptyListWhenContentIsBlank() {
        assertTrue(splitter.split(null).isEmpty());
        assertTrue(splitter.split("").isEmpty());
        assertTrue(splitter.split("   ").isEmpty());
        assertTrue(splitter.split("\n\n\t").isEmpty());
    }

    @Test
    @DisplayName("普通短文本应该返回一个切片，并自动去除首尾空白")
    void shouldReturnSingleChunkWhenContentIsShort() {
        String content = "   这是一个普通的知识库文档内容，用于测试短文本切片。   ";

        List<String> chunks = splitter.split(content);

        assertEquals(1, chunks.size());
        assertEquals("这是一个普通的知识库文档内容，用于测试短文本切片。", chunks.get(0));
    }

    @Test
    @DisplayName("多个较长段落应该按照空行切分成多个切片")
    void shouldSplitLongParagraphsByBlankLine() {
        String firstParagraph = "这是第一段内容，用于模拟知识库文档中的一个完整段落。".repeat(4);
        String secondParagraph = "这是第二段内容，用于模拟知识库文档中的另一个完整段落。".repeat(4);

        String content = firstParagraph + "\n\n" + secondParagraph;

        List<String> chunks = splitter.split(content);

        assertEquals(2, chunks.size());
        assertEquals(firstParagraph, chunks.get(0));
        assertEquals(secondParagraph, chunks.get(1));
    }

    @Test
    @DisplayName("过短段落应该合并，避免切片过碎")
    void shouldMergeShortParagraphs() {
        String content = """
                短段落一
                
                短段落二
                
                这是一个相对较长的段落，用于让前面的短段落和当前段落合并后形成更完整的语义内容。
                """;

        List<String> chunks = splitter.split(content);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("短段落一"));
        assertTrue(chunks.get(0).contains("短段落二"));
        assertTrue(chunks.get(0).contains("这是一个相对较长的段落"));
    }

    @Test
    @DisplayName("超长文本应该按固定窗口切成多个切片")
    void shouldSplitLongTextIntoMultipleChunks() {
        String content = "A".repeat(1700);

        List<String> chunks = splitter.split(content);

        assertEquals(3, chunks.size());

        assertEquals(800, chunks.get(0).length());
        assertEquals(800, chunks.get(1).length());
        assertEquals(300, chunks.get(2).length());
    }

    @Test
    @DisplayName("超长文本切片之间应该保留重叠上下文")
    void shouldKeepOverlapBetweenLongTextChunks() {
        String content = buildLongText(1700);

        List<String> chunks = splitter.split(content);

        String firstChunk = chunks.get(0);
        String secondChunk = chunks.get(1);

        String firstChunkTail = firstChunk.substring(700, 800);
        String secondChunkHead = secondChunk.substring(0, 100);

        assertEquals(firstChunkTail, secondChunkHead);
    }

    @Test
    @DisplayName("Windows 换行符应该被标准化处理")
    void shouldNormalizeWindowsLineBreaks() {
        String firstParagraph = "这是第一段内容，用于测试 Windows 换行符处理。".repeat(4);
        String secondParagraph = "这是第二段内容，用于测试 Windows 换行符处理。".repeat(4);

        String content = firstParagraph + "\r\n\r\n" + secondParagraph;

        List<String> chunks = splitter.split(content);

        assertEquals(2, chunks.size());
        assertEquals(firstParagraph, chunks.get(0));
        assertEquals(secondParagraph, chunks.get(1));
    }

    private String buildLongText(int length) {
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            char ch = (char) ('A' + i % 26);
            builder.append(ch);
        }

        return builder.toString();
    }
}