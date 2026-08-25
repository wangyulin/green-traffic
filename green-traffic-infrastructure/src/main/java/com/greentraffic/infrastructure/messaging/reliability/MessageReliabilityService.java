package com.greentraffic.infrastructure.messaging.reliability;

import com.greentraffic.core.port.output.messaging.Message;

/**
 * 消息可靠性服务：提供幂等、重试与 DLQ 钩子（基础骨架）
 */
public interface MessageReliabilityService {

    /** 是否为重复消息（已处理） */
    boolean isDuplicate(Message<?> message);

    /** 标记消息为已发送/已处理 */
    void markSent(Message<?> message);

    /** 处理发送失败达到重试上限后的 DLQ 逻辑 */
    void handleDlq(Message<?> message, Throwable t);

    /** 获取最大重试次数 */
    int getMaxRetries();
}
