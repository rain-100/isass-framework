package vip.isass.framework.common.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public final class IsassServiceLoader {

    private IsassServiceLoader() {
    }

    public static <T> List<T> load(Class<T> serviceType) {
        return load(serviceType, Thread.currentThread().getContextClassLoader());
    }

    public static <T> List<T> load(Class<T> serviceType, ClassLoader classLoader) {
        ClassLoader loader = classLoader == null ? IsassServiceLoader.class.getClassLoader() : classLoader;
        return ServiceLoader.load(serviceType, loader)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    public static <T> Optional<T> loadFirst(Class<T> serviceType) {
        return loadFirst(serviceType, Thread.currentThread().getContextClassLoader());
    }

    public static <T> Optional<T> loadFirst(Class<T> serviceType, ClassLoader classLoader) {
        ClassLoader loader = classLoader == null ? IsassServiceLoader.class.getClassLoader() : classLoader;
        return ServiceLoader.load(serviceType, loader)
                .stream()
                .map(ServiceLoader.Provider::get)
                .findFirst();
    }

    public static <T> List<T> mergeByClass(Collection<? extends T> first,
                                           Collection<? extends T> second) {
        Map<Class<?>, T> merged = new LinkedHashMap<>();
        putAll(merged, first);
        putAll(merged, second);
        return new ArrayList<>(merged.values());
    }

    private static <T> void putAll(Map<Class<?>, T> merged, Collection<? extends T> services) {
        for (T service : services) {
            if (service != null) {
                merged.putIfAbsent(service.getClass(), service);
            }
        }
    }
}
