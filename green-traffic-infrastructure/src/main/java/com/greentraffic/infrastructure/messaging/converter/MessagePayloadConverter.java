package com.greentraffic.infrastructure.messaging.converter;

import com.greentraffic.core.port.output.messaging.Message;

import java.util.Set;

public interface MessagePayloadConverter<T> {

    /**
     * 判断当前 Converter 是否能够处理该消息。
     */
    boolean supports(Message<?> message);

    /**
     * 将消息 Payload 转换成目标类型。
     */
    Message<T> convert(Message<?> message);

    /**
     * 支持的消息类型。
     */
    Set<String> supportedMessageTypes();
}
