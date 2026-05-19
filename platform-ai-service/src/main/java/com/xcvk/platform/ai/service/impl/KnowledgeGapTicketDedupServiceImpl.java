package com.xcvk.platform.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcvk.platform.ai.model.entity.AiKnowledgeGapTicket;
import com.xcvk.platform.ai.model.internal.KnowledgeGapTicketDecision;
import com.xcvk.platform.ai.model.vo.AssistantTicketVO;
import com.xcvk.platform.ai.repository.mapper.AiKnowledgeGapTicketMapper;
import com.xcvk.platform.ai.service.KnowledgeGapTicketDedupService;
import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 知识缺口转工单去重服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGapTicketDedupServiceImpl implements KnowledgeGapTicketDedupService {

    private final AiKnowledgeGapTicketMapper knowledgeGapTicketMapper;

    private final SnowflakeIdGenerator idGenerator;

    @Override
    public Optional<AiKnowledgeGapTicket> findByRawQuestion(String rawQuestion) {
        String hash = hashQuestion(rawQuestion);

        AiKnowledgeGapTicket record = knowledgeGapTicketMapper.selectOne(
                new LambdaQueryWrapper<AiKnowledgeGapTicket>()
                        .eq(AiKnowledgeGapTicket::getRawQuestionHash, hash)
                        .last("LIMIT 1")
        );

        return Optional.ofNullable(record);
    }

    @Override
    public Optional<AiKnowledgeGapTicket> findByNormalizedQuestion(KnowledgeGapTicketDecision decision) {
        if (decision == null) {
            return Optional.empty();
        }

        String dedupText = StringUtils.hasText(decision.dedupKey())
                ? decision.dedupKey()
                : decision.normalizedQuestion();

        if (!StringUtils.hasText(dedupText)) {
            return Optional.empty();
        }

        String hash = hashQuestion(dedupText);

        AiKnowledgeGapTicket record = knowledgeGapTicketMapper.selectOne(
                new LambdaQueryWrapper<AiKnowledgeGapTicket>()
                        .eq(AiKnowledgeGapTicket::getNormalizedQuestionHash, hash)
                        .last("LIMIT 1")
        );

        return Optional.ofNullable(record);
    }

    @Override
    public void recordCreatedTicket(Long userId,
                                    String rawQuestion,
                                    KnowledgeGapTicketDecision decision,
                                    AssistantTicketVO ticket) {

        if (decision == null || ticket == null || ticket.ticketId() == null) {
            return;
        }

        String dedupText = StringUtils.hasText(decision.dedupKey())
                ? decision.dedupKey()
                : decision.normalizedQuestion();

        AiKnowledgeGapTicket record = new AiKnowledgeGapTicket()
                .setId(idGenerator.nextId())
                .setRawQuestion(rawQuestion)
                .setRawQuestionHash(hashQuestion(rawQuestion))
                .setNormalizedQuestion(decision.normalizedQuestion())
                .setNormalizedQuestionHash(hashQuestion(dedupText))
                .setTicketId(ticket.ticketId())
                .setTicketNo(ticket.ticketNo())
                .setTicketStatus(ticket.status())
                .setTicketTypeCode(ticket.ticketTypeCode())
                .setTicketTitle(ticket.title())
                .setCreatedBy(userId)
                .setHitCount(1);

        try {
            knowledgeGapTicketMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            /*
             * MVP 先不抛异常。
             *
             * 这里表示刚创建完工单后，插入去重记录时发现已有重复记录。
             * 后续高并发版本可以在创建工单前加 Redis Lock 或 DB Reserve。
             */
            log.warn("知识缺口工单去重记录重复，rawQuestion={}, normalizedQuestion={}",
                    rawQuestion,
                    decision.normalizedQuestion()
            );
        }
    }

    @Override
    public void increaseHitCount(Long id) {
        if (id == null) {
            return;
        }

        AiKnowledgeGapTicket record = knowledgeGapTicketMapper.selectById(id);

        if (record == null) {
            return;
        }

        Integer hitCount = record.getHitCount() == null ? 0 : record.getHitCount();

        record.setHitCount(hitCount + 1);

        knowledgeGapTicketMapper.updateById(record);
    }

    private String hashQuestion(String question) {
        String normalized = normalizeForHash(question);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("问题 Hash 计算失败", ex);
        }
    }

    private String normalizeForHash(String question) {
        if (!StringUtils.hasText(question)) {
            return "";
        }

        String normalized = question
                .trim()
                .toLowerCase();

        // 统一中英文标点、空白
        normalized = normalized
                .replaceAll("\\s+", "")
                .replaceAll("[，。！？、,.!?；;：:（）()【】\\[\\]「」“”\"'`]", "");

        // 去掉对语义影响很小、但容易导致 hash 不一致的虚词
        normalized = normalized
                .replace("的", "")
                .replace("了", "")
                .replace("一下", "")
                .replace("请问", "")
                .replace("请", "");

        // 统一常见表达
        normalized = normalized
                .replace("请提供", "")
                .replace("请告知", "")
                .replace("帮我看下", "")
                .replace("帮忙看下", "")
                .replace("这种情况应该走什么内部处理流程", "处理流程")
                .replace("这种情况应该走什么处理流程", "处理流程")
                .replace("内部处理流程", "处理流程")
                .replace("内部故障处理或报修流程", "处理流程")
                .replace("处理流程与报修方式", "处理流程")
                .replace("处理流程和报修方式", "处理流程");

        return normalized;
    }
}