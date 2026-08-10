// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.adapter.springboot.converter;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import vip.isass.framework.common.support.Converter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class IsassSpringConverterAdapter implements ConditionalGenericConverter {

    private final List<ConverterDescriptor> converters;

    public IsassSpringConverterAdapter(Collection<Converter<?, ?>> converters) {
        this.converters = converters.stream()
            .map(ConverterDescriptor::new)
            .toList();
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        Set<ConvertiblePair> convertiblePairs = new LinkedHashSet<>();
        for (ConverterDescriptor converter : converters) {
            convertiblePairs.add(new ConvertiblePair(converter.sourceType(), converter.targetType()));
        }
        return convertiblePairs;
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return converters.stream().anyMatch(converter -> converter.matches(sourceType, targetType));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }

        for (ConverterDescriptor converter : converters) {
            if (converter.matches(sourceType, targetType) && converter.supports(source, targetType)) {
                return converter.convert(source);
            }
        }
        return source;
    }

    private record ConverterDescriptor(Converter<?, ?> converter, Class<?> sourceType, Class<?> targetType) {

        private ConverterDescriptor(Converter<?, ?> converter) {
            this(converter, resolveType(converter, 0), resolveType(converter, 1));
        }

        private boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return this.sourceType.isAssignableFrom(sourceType.getObjectType())
                && converter.supportTargetClass(targetType.getObjectType());
        }

        private boolean supports(Object source, TypeDescriptor targetType) {
            return converter.supportSourceType(source)
                && converter.supportTargetClass(targetType.getObjectType());
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private Object convert(Object source) {
            return ((Converter) converter).convert(source);
        }

        private static Class<?> resolveType(Converter<?, ?> converter, int genericIndex) {
            ResolvableType type = ResolvableType.forClass(converter.getClass()).as(Converter.class);
            Class<?> resolved = type.getGeneric(genericIndex).resolve();
            return resolved == null ? Object.class : resolved;
        }
    }
}
