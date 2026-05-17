package com.xcvk.platform.ai.trace.service.impl;

import com.xcvk.platform.ai.model.vo.trace.RagTraceTimelineVO;
import com.xcvk.platform.ai.trace.document.RagTraceDocument;
import com.xcvk.platform.ai.trace.mapper.RagTraceDocumentMapper;
import com.xcvk.platform.ai.trace.mapper.RagTraceTimelineMapper;
import com.xcvk.platform.ai.trace.model.RagTraceContext;
import com.xcvk.platform.ai.trace.repository.RagTraceRepository;
import com.xcvk.platform.ai.trace.service.RagTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 *   rag 轨迹服务实现
 * */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagTraceServiceImpl
        implements RagTraceService {

    private final RagTraceRepository repository;

    @Override
    public void save(RagTraceContext context) {

        try {

            RagTraceDocument document =
                    RagTraceDocumentMapper.toDocument(context);

            repository.save(document);

        } catch (Exception e) {

            log.error(
                    "save rag trace failed",
                    e
            );
        }
    }

    @Override
    public RagTraceTimelineVO getTimeline(
            Long executionLogId
    ) {

        RagTraceDocument document =
                repository.findByExecutionLogId(executionLogId)
                        .orElse(null);

        return RagTraceTimelineMapper.toTimeline(document);
    }
}