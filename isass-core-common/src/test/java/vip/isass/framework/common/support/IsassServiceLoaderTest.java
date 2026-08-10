// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.converter.StringToMapConverter;
import vip.isass.framework.common.converter.datatime.StringToLocalDateConverter;
import vip.isass.framework.common.exception.BuildInCoreExceptionMapping;
import vip.isass.framework.common.exception.IExceptionMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IsassServiceLoaderTest {

    @Test
    void loadsServicesFromMetaInfServices() {
        List<Converter> converters = IsassServiceLoader.load(Converter.class);

        assertThat(converters)
                .extracting(Object::getClass)
                .contains(StringToLocalDateConverter.class, StringToMapConverter.class);
    }

    @Test
    void loadsExceptionMappingsFromMetaInfServices() {
        List<IExceptionMapping> exceptionMappings = IsassServiceLoader.load(IExceptionMapping.class);

        assertThat(exceptionMappings)
                .extracting(Object::getClass)
                .contains(BuildInCoreExceptionMapping.class);
    }

    @Test
    void mergesServicesByClassKeepingFirstCollectionFirst() {
        Converter<String, String> first = new TestStringConverter("first");
        Converter<String, String> duplicate = new TestStringConverter("duplicate");
        Converter<Object, Object> second = new TestObjectConverter();

        List<Converter> merged = IsassServiceLoader.mergeByClass(
                List.of(first, second),
                List.of(duplicate)
        );

        assertThat(merged).containsExactly(first, second);
    }

    private record TestStringConverter(String prefix) implements Converter<String, String> {

        @Override
        public boolean supportSourceType(Object source) {
            return source instanceof String;
        }

        @Override
        public boolean supportTargetClass(Class clazz) {
            return String.class.isAssignableFrom(clazz);
        }

        @Override
        public String convert(String source) {
            return prefix + ":" + source;
        }
    }

    private static class TestObjectConverter implements Converter<Object, Object> {

        @Override
        public boolean supportSourceType(Object source) {
            return true;
        }

        @Override
        public boolean supportTargetClass(Class clazz) {
            return true;
        }

        @Override
        public Object convert(Object source) {
            return source;
        }
    }
}
