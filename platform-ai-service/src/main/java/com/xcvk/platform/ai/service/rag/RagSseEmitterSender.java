package com.xcvk.platform.ai.service.rag;

import com.xcvk.platform.ai.model.vo.RagCitation;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * RAG SSE 事件发送器。
 */
@Component
public class RagSseEmitterSender {

    private static final String SSE_EVENT_CONTEXTS = "contexts";

    private static final String SSE_EVENT_DELTA = "delta";

    private static final String SSE_EVENT_DONE = "done";

    private static final String SSE_EVENT_ERROR = "error";

    private static final String SSE_EVENT_REJECTED = "rejected";

    private static final String SSE_DONE_FLAG = "[DONE]";

    private static final long SSE_TIMEOUT_MS = 180_000L;

    private final Object emitterLock = new Object();

    public SseEmitter createEmitter() {
        return new SseEmitter(SSE_TIMEOUT_MS);
    }

    public void sendContexts(SseEmitter emitter, List<RagCitation> citations) {
        sendEvent(emitter, SSE_EVENT_CONTEXTS, citations);
    }

    public void sendDelta(SseEmitter emitter, String delta) {
        sendEvent(emitter, SSE_EVENT_DELTA, delta);
    }

    public void sendDone(SseEmitter emitter) {
        sendEvent(emitter, SSE_EVENT_DONE, SSE_DONE_FLAG);
    }

    public void sendError(SseEmitter emitter, String message) {
        sendEvent(emitter, SSE_EVENT_ERROR, message);
    }

    public void sendRejectEvents(SseEmitter emitter, String answer, String rejectReason) {
        sendEvent(emitter, SSE_EVENT_REJECTED, rejectReason);
        sendEvent(emitter, SSE_EVENT_DELTA, answer);
        sendEvent(emitter, SSE_EVENT_DONE, SSE_DONE_FLAG);
        emitter.complete();
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        synchronized (emitterLock) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(data)
                );
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }
    }
}