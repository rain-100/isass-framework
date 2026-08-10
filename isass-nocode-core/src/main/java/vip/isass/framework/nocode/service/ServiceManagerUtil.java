// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.common.support.api.ApiOrder;
import vip.isass.framework.common.support.api.IsassOrderUtil;
import vip.isass.framework.nocode.UnimplementedMethodException;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class ServiceManagerUtil {

    public static <S, V> V applyUntilNotNull(List<S> services, Function<S, V> function) {
        checkServices(services);

        boolean hasLocalService = false;
        for (S service : services) {
            // IServiceManager 应该作为调用方， IServiceManager 不可能被 spring 注入到 services 列表
            if (service instanceof IServiceManager) {
                continue;
            }

            // controller 的实现无需执行
            int order = IsassOrderUtil.getOrder(service);
            if (order == ApiOrder.CONTROLLER) {
                continue;
            }

            // 如果有本地服务，则无需执行 feign 服务
            if (service instanceof ILocalService) {
                hasLocalService = true;
            }

            if (hasLocalService && (order == ApiOrder.FEIGN_SERVICE)) {
                continue;
            }

            try {
                V value = function.apply(service);
                if (value != null) {
                    return value;
                }
            } catch (UnimplementedMethodException e) {
                // ignore
            }
        }
        return null;
    }

    public static <S> void consume(List<S> services, Consumer<S> consumer) {
        checkServices(services);

        for (S service : services) {
            try {
                consumer.accept(service);
                return;
            } catch (UnimplementedMethodException e) {
                // ignore
            }
        }
    }

    public static <S> void consumeWithoutException(List<S> services, Consumer<S> consumer) {
        checkServices(services);

        for (S service : services) {
            try {
                consumer.accept(service);
                return;
            } catch (UnimplementedMethodException e) {
                // ignore
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static <S> void checkServices(List<S> services) {
        if (services == null) {
            throw new UnsupportedOperationException("接口实现类列表为 null，请复制错误日志供开发人员排查错误");
        }
    }
}
