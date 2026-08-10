// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

/**
 * 面向跨聚合用例的应用服务合同。
 *
 * <p>应用服务只声明显式的业务操作，不继承 {@link IService} 的标准 CRUD 操作。</p>
 */
public interface IApplicationService {

    default String entity() {
        String name = applicationInterface().getSimpleName().replaceFirst("^I", "").replaceFirst("Service$", "");
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    default String service() {
        String packageName = applicationInterface().getPackageName();
        // ServiceInfo belongs to the microservice root package, not a bounded context package.
        while (!packageName.isEmpty()) {
            try {
                Class<?> serviceInfo = Class.forName(packageName + ".ServiceInfo");
                return (String) serviceInfo.getField("SERVICE_FULL_NAME").get(null);
            } catch (ClassNotFoundException ignored) {
                int separator = packageName.lastIndexOf('.');
                packageName = separator < 0 ? "" : packageName.substring(0, separator);
            } catch (ReflectiveOperationException ignored) {
                break;
            }
        }
        String[] parts = applicationInterface().getPackageName().split("\\.");
        return parts.length >= 3 ? parts[2] + "-service" : "application";
    }

    private Class<?> applicationInterface() {
        Class<?> type = getClass();
        while (type != null && type != Object.class) {
            for (Class<?> candidate : type.getInterfaces()) {
                Class<?> resolved = findApplicationInterface(candidate);
                if (resolved != null) {
                    return resolved;
                }
            }
            type = type.getSuperclass();
        }
        throw new IllegalStateException("Cannot resolve IApplicationService interface: " + getClass().getName());
    }

    private static Class<?> findApplicationInterface(Class<?> type) {
        if (type != IApplicationService.class && type != ILocalApplicationService.class
                && IApplicationService.class.isAssignableFrom(type)) {
            return type;
        }
        for (Class<?> parent : type.getInterfaces()) {
            Class<?> resolved = findApplicationInterface(parent);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }
}
