package com.greentraffic.common.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 统一消息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message<T> implements Serializable {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 消息类型（用于路由）
     */
    private String messageType;

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息标签（RocketMQ专用）
     */
    private String tag;

    /**
     * 消息键（用于分区）
     */
    private String key;

    /**
     * 消息体
     */
    private T payload;

    /**
     * 消息头
     */
    private Map<String, String> headers;

    /**
     * 发送时间
     */
    private LocalDateTime timestamp;

    /**
     * 创建消息
     */
    public static <T> Message<T> of(String messageType, T payload) {
        return Message.<T>builder()
                .messageId(UUID.randomUUID().toString())
                .messageType(messageType)
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
