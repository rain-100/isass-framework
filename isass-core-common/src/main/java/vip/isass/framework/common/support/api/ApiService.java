// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * API 服务接口
 *
 * @author isass
 */
public interface ApiService {

    Logger LOGGER = LoggerFactory.getLogger(ApiService.class);

    default <S, V> V apply(Collection<S> services, Function<S, V> function) {
        if (services == null) {
            throw new UnsupportedOperationException("当前环境没有" + this.getClass().getSimpleName());
        }

        boolean hasLocalService = false;
        for (S service : services) {
            // 如果有本地服务，则无需执行 feign 服务
            int order = IsassOrderUtil.getOrder(service);
            if (order == ApiOrder.LOCAL_SERVICE) {
                hasLocalService = true;
            }

            if (order == ApiOrder.FEIGN_SERVICE && hasLocalService) {
                continue;
            }

            V value = function.apply(service);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    default <S> void consume(Collection<S> services, Consumer<S> consumer) {
        if (services == null) {
            throw new UnsupportedOperationException("当前环境没有" + this.getClass().getSimpleName());
        }

        for (S service : services) {
            consumer.accept(service);
            return;
        }
    }

    default <S> void consumeWithoutException(Collection<S> services, Consumer<S> consumer) {
        if (services == null) {
            throw new UnsupportedOperationException("当前环境没有" + this.getClass().getSimpleName());
        }

        for (S service : services) {
            try {
                consumer.accept(service);
                return;
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

}
