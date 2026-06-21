package vip.isass.framework.adapter.springboot.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

import java.util.Map;

class OnIsassFeatureCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(
                ConditionalOnIsassFeature.class.getName());
        if (attributes == null) {
            return true;
        }
        IsassFeature[] features = (IsassFeature[]) attributes.get("value");
        for (IsassFeature feature : features) {
            if (!ClassUtils.isPresent(feature.getMarkerClassName(), context.getClassLoader())) {
                return false;
            }
        }
        return true;
    }
}
