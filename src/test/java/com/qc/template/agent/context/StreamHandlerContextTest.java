package com.qc.template.agent.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertSame;

class StreamHandlerContextTest {

    private static final String TASK_ID = "image-progress-task";

    @AfterEach
    void cleanup() {
        StreamHandlerContext.clear(TASK_ID);
    }

    @Test
    void resolvesHandlerByTaskAcrossAsyncExecutionBoundaries() {
        Consumer<String> handler = message -> { };

        StreamHandlerContext.set(TASK_ID, handler);

        assertSame(handler, StreamHandlerContext.get(TASK_ID));
    }
}
