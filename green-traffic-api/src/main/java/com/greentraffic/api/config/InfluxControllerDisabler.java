package com.greentraffic.api.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "traffic.influxdb", name = "enabled", havingValue = "false")
public class InfluxControllerDisabler implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String targetClass = "com.greentraffic.api.controller.InfluxTestController";

        for (String name : beanFactory.getBeanDefinitionNames()) {
            try {
                BeanDefinition bd = beanFactory.getBeanDefinition(name);
                String beanClassName = bd.getBeanClassName();
                if (targetClass.equals(beanClassName) || "influxTestController".equals(name)) {
                    if (beanFactory instanceof BeanDefinitionRegistry registry) {
                        registry.removeBeanDefinition(name);
                    }
                }
            } catch (NoSuchBeanDefinitionException ignored) {
            }
        }
    }
}
