// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.serialization.impl.protobuf2;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.ByteString;

import java.lang.reflect.Method;

/**
 * @author Rain
 */
public class ProtobufMethodCache {

    public static final LoadingCache<String, Method> PARSE_METHOD_CACHE = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build(new CacheLoader<String, Method>() {
            @Override
            public Method load(String protoName) throws Exception {
                Class<?> protoClass = Class.forName(protoName);
                Method parseMethod = protoClass.getMethod("parseFrom", ByteString.class);
                PARSE_METHOD_CACHE.put(protoName, parseMethod);
                return parseMethod;
            }
        });

    public static final LoadingCache<String, Method> BUILDER_METHOD_CACHE = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build(new CacheLoader<String, Method>() {
            @Override
            public Method load(String protoName) throws Exception {
                Class<?> protoClass = Class.forName(protoName);
                Method builderMethod = protoClass.getMethod("newBuilder");
                BUILDER_METHOD_CACHE.put(protoName, builderMethod);
                return builderMethod;
            }
        });
}
