// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import java.util.Collection;
import java.util.Collections;

public interface BeanProvider {

    default Object getContext() {
        return null;
    }

    default boolean isInitialized() {
        return getContext() != null;
    }

    default <T> T addBean(Class<T> beanClass) {
        throw new UnsupportedOperationException("当前环境不支持动态注册 bean");
    }

    default Object getBean(String name) {
        throw new UnsupportedOperationException("当前环境不支持通过名称获取 bean");
    }

    default <T> T getBean(String name, Class<T> requiredType) {
        throw new UnsupportedOperationException("当前环境不支持通过名称和类型获取 bean");
    }

    default <T> T getBean(Class<T> requiredType) {
        throw new UnsupportedOperationException("当前环境不支持通过类型获取 bean");
    }

    default <T> Collection<T> getBeans(Class<T> requiredType) {
        return Collections.emptyList();
    }

    default <T, P> T getBean(Class<T> requiredType, Class<P> type) {
        return null;
    }

    default <T> T getBean(Class<T> requiredType, Object... objects) {
        throw new UnsupportedOperationException("当前环境不支持通过构造参数获取 bean");
    }

    default void unRegistryBean(String beanName) {
    }

    default void unRegistryBean(Class<?> beanClass) {
    }

    default String[] getBeanNamesForType(Class<?> type) {
        return new String[0];
    }
}
