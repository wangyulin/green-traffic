package com.greentraffic.infrastructure.messaging.rocketmq.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Configuration;

/**
 * 调整 RocketMQ 相关自动配置 Bean 的角色为 ROLE_INFRASTRUCTURE，
 * 避免被 BeanPostProcessorChecker 警告为未被全部后处理器处理。
 */
@Configuration
public class RocketMqBeanRoleAdjuster implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(name);
            String beanClassName = bd.getBeanClassName();
            if (beanClassName == null) continue;

            // 针对 RocketMQ Spring 自动配置类和相关组件进行标记
            if (beanClassName.contains("rocketmq") || beanClassName.contains("RocketMQ")) {
                bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            }
        }
    }
}
