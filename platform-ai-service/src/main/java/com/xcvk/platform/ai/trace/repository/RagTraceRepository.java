package com.xcvk.platform.ai.trace.repository;

import com.xcvk.platform.ai.trace.document.RagTraceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 *  rag trace
 * */
public interface RagTraceRepository
        extends MongoRepository<RagTraceDocument, String> {
}