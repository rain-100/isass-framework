// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.property;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * 从可序列化 getter 方法引用中提取 Java 属性名。
 */
public final class PropertyNameResolver {

    private PropertyNameResolver() {
    }

    public static <T, R> String resolve(PropertyGetter<T, R> getter) {
        SerializedLambda lambda = serializedLambda(getter);
        String methodName = lambda.getImplMethodName();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Introspector.decapitalize(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Introspector.decapitalize(methodName.substring(2));
        }
        throw new IllegalArgumentException("属性引用必须是 getXxx 或 isXxx 方法引用：" + methodName);
    }

    private static SerializedLambda serializedLambda(Serializable lambda) {
        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(lambda);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("无法解析属性方法引用", e);
        }
    }
}
