package com.greentraffic.infrastructure.messaging.springevents;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.core.task.TaskExecutor;
import org.springframework.context.ApplicationContext;

/**
 * Spring Events 消息发布者
 * 用于开发环境
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "messaging.type", havingValue = "events", matchIfMissing = true)
@RequiredArgsConstructor
public class SpringEventsMessagePublisher implements MessagePublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    // 用 ApplicationContext 在运行时选择合适的 TaskExecutor（优先 messageTaskExecutor，再 applicationTaskExecutor）
    private final ApplicationContext applicationContext;

    @Override
    public void publish(Message<?> message) {
        log.debug("Spring Events 发布消息: {}", message.getMessageType());
        applicationEventPublisher.publishEvent(new MessageEvent(this, message));
    }

    @Override
    public void publish(String topic, Message<?> message) {
        message.setTopic(topic);
        publish(message);
    }

    @Override
    public void publishAsync(Message<?> message) {
        try {
            TaskExecutor executor = resolveTaskExecutor();
            if (executor != null) {
                log.debug("Spring Events 异步发布消息: {}", message.getMessageType());
                executor.execute(() -> applicationEventPublisher.publishEvent(new MessageEvent(this, message)));
                return;
            }
        } catch (Exception ex) {
            log.warn("选择 TaskExecutor 时出现问题，回退到同步发布", ex);
        }
        publish(message);
    }

    private TaskExecutor resolveTaskExecutor() {
        // 首先尝试名为 messageTaskExecutor 的 bean（优先用于消息异步）
        if (applicationContext.containsBean("messageTaskExecutor")) {
            return applicationContext.getBean("messageTaskExecutor", TaskExecutor.class);
        }
        // 其次尝试 applicationTaskExecutor
        if (applicationContext.containsBean("applicationTaskExecutor")) {
            return applicationContext.getBean("applicationTaskExecutor", TaskExecutor.class);
        }
        // 最后尝试任意一个 TaskExecutor
        String[] names = applicationContext.getBeanNamesForType(TaskExecutor.class);
        if (names == null || names.length == 0) {
            return null;
        }
        return applicationContext.getBean(names[0], TaskExecutor.class);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
