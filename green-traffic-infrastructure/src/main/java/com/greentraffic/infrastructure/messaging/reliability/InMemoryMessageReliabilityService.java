package com.greentraffic.infrastructure.messaging.reliability;

import com.greentraffic.core.port.output.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开发/测试用的内存实现：记录已发送 messageId，并提供简单的 DLQ 处理（仅记录日志）。
 */
@Slf4j
@Component
public class InMemoryMessageReliabilityService implements MessageReliabilityService {

    private final Set<String> sent = ConcurrentHashMap.newKeySet();

    private final int maxRetries;

    public InMemoryMessageReliabilityService(@Value("${messaging.reliability.max-retries:2}") int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean isDuplicate(Message<?> message) {
        if (message == null || message.getMessageId() == null) return false;
        return sent.contains(message.getMessageId());
    }

    @Override
    public void markSent(Message<?> message) {
        if (message == null || message.getMessageId() == null) return;
        sent.add(message.getMessageId());
    }

    @Override
    public void handleDlq(Message<?> message, Throwable t) {
        log.warn("DLQ message {} due to: {}", message == null ? null : message.getMessageId(), t == null ? "null" : t.toString());
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }
}
