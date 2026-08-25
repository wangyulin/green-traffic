package com.greentraffic.core.port.output.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 统一消息模型（迁移到 core 的 Port 层，作为消息契约）
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
     * 消息协议/Schema 版本
     */
    private String schemaVersion;

    /**
     * 消息来源（例如 sumo, simulator, sensor）
     */
    private String source;

    /**
     * 分布式追踪 ID
     */
    private String traceId;

    /**
     * 关联 ID（用于请求链路关联）
     */
    private String correlationId;

    /**
     * 创建消息
     */
    public static <T> Message<T> of(String messageType, T payload) {
        return Message.<T>builder()
                .messageId(UUID.randomUUID().toString())
                .messageType(messageType)
                .schemaVersion("1.0")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public <R> Message<R> withPayload(R newPayload) {
        return Message.<R>builder()
                .messageId(this.messageId)
                .messageType(this.messageType)
                .topic(this.topic)
                .tag(this.tag)
                .key(this.key)
                .payload(newPayload)
                .headers(this.headers)
                .timestamp(this.timestamp)
                .schemaVersion(this.schemaVersion)
                .source(this.source)
                .traceId(this.traceId)
                .correlationId(this.correlationId)
                .build();
    }
}
