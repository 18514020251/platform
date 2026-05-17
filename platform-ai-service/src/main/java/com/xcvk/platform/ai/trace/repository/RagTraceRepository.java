package com.xcvk.platform.ai.trace.repository;

import com.xcvk.platform.ai.trace.document.RagTraceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 *  rag trace
 * */
public interface RagTraceRepository
        extends MongoRepository<RagTraceDocument, String> {

    Optional<RagTraceDocument> findByExecutionLogId(
            Long executionLogId
    );
}