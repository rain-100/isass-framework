// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.spring.event.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import vip.isass.framework.mq.core.FailStrategy;
import vip.isass.framework.mq.core.MqMessageContext;
import vip.isass.framework.mq.core.consumer.IMqConsumer;
import vip.isass.framework.mq.core.consumer.MqConsumerManager;
import vip.isass.framework.mq.spring.event.IsassMqEvent;
import vip.isass.framework.mq.spring.event.SpringEventConfiguration;
import vip.isass.framework.mq.spring.event.SpringEventConst;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SpringEventMqConsumerManager implements ApplicationListener<IsassMqEvent>, MqConsumerManager {

    @Autowired(required = false)
    private List<IMqConsumer> mqConsumers;

    @Resource
    private SpringEventConfiguration springEventConfiguration;

    @Override
    public void onApplicationEvent(IsassMqEvent event) {
        if (CollUtil.isEmpty(mqConsumers)) {
            return;
        }

        MqMessageContext mqMessageContext = (MqMessageContext) event.getSource();
        mqConsumers.stream()
            // 判断厂商
            .filter(mc -> StrUtil.isBlank(mqMessageContext.getManufacturer())
                || StrUtil.isBlank(mc.getManufacturer())
                || mc.getManufacturer().equals(mqMessageContext.getManufacturer()))

            // 判断 topic
            .filter(mc -> mc.getTopic().equals(mqMessageContext.getTopic()))

            // 判断 tag
            .filter(mc -> "*".equals(mc.getTag()) || mc.getTag().equals(mqMessageContext.getTag()))

            .forEach(mc -> {
                try {
                    doConsume(mc, mqMessageContext);
                } catch (Exception e) {
                    log.error("springEvent消费异常，请业务视情况处理异常");
                    throw e;
                }
            });
    }

    private void doConsume(IMqConsumer mqConsumer, MqMessageContext mqMessageContext) {
        try {
            mqConsumer.consume(mqMessageContext);
        } catch (Exception e) {
            Throwable unwrap = ExceptionUtil.unwrap(e);
            log.error("mq消费错误：{}", unwrap.getMessage(), unwrap);

            FailStrategy failStrategy = mqConsumer.getFailStrategy();
            switch (failStrategy) {
                case IGNORE:
                    log.info("忽略消费异常");
                    return;
                case RETRY:
                    int maxRetryCount = 3;
                    for (int i = 0; i < maxRetryCount; i++) {
                        log.info("正在开始第{}次重试消费。重试最大次数为{}", i + 1, maxRetryCount);
                        try {
                            mqConsumer.consume(mqMessageContext);
                            return;
                        } catch (Exception e1) {
                            log.error("重试消费错误: {}", e1.getMessage(), e1);
                        }
                    }
                    log.info("超过最大重试次数，将视为正常消费");
                    return;
                case RETRY_IMMEDIATELY:
                    for (int i = 0; i < mqConsumer.getImmediatelyRetryCount(); i++) {
                        log.info("正在开始第{}次重试消费。重试最大次数为{}", i + 1, mqConsumer.getImmediatelyRetryCount());
                        try {
                            mqConsumer.consume(mqMessageContext);
                        } catch (Exception e1) {
                            log.error("重试消费错误: {}", e1.getMessage(), e1);
                        }
                    }
                    log.info("超过最大立即重试次数，将视为正常消费");
                    return;
                default:
                    log.error("未实现[{}]的失败重试策略逻辑，将视为正常消费", failStrategy);
            }
        }
    }

    @Override
    public String getManufacturer() {
        return SpringEventConst.MANUFACTURER;
    }

    @Override
    public void subscribe() {
        if (CollUtil.isEmpty(mqConsumers)) {
            return;
        }
        mqConsumers = mqConsumers.stream()
            // 判断厂商
            .filter(mc -> StrUtil.isBlank(mc.getManufacturer()) || mc.getManufacturer().equals(getManufacturer()))
            .collect(Collectors.toList());
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isEnable() {
        return springEventConfiguration.isEnable();
    }

}
