// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Finds annotated entrypoint interfaces and registers a remote proxy only when no local implementation exists. */
public final class EntrypointProxyRegistrar
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    @SuppressWarnings("unchecked")
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;
        for (String basePackage : scanPackages()) {
            for (BeanDefinition candidate : scanner().findCandidateComponents(basePackage)) {
                try {
                    Class<?> type = Class.forName(candidate.getBeanClassName(), false,
                            Thread.currentThread().getContextClassLoader());
                    if (!type.isInterface() || !IEntrypoint.class.isAssignableFrom(type)) {
                        continue;
                    }
                    Class<? extends IEntrypoint> entrypointType = (Class<? extends IEntrypoint>) type;
                    if (beanFactory.getBeanNamesForType(entrypointType, false, false).length > 0) {
                        continue;
                    }
                    String beanName = entrypointType.getName();
                    registry.registerBeanDefinition(beanName,
                            BeanDefinitionBuilder.genericBeanDefinition(EntrypointProxyFactoryBean.class)
                                    .addConstructorArgValue(entrypointType)
                                    .getBeanDefinition());
                } catch (ClassNotFoundException exception) {
                    throw new IllegalStateException("无法加载 Entrypoint: " + candidate.getBeanClassName(), exception);
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private Set<String> scanPackages() {
        String configured = environment.getProperty("isass.entrypoint.scan-packages", "vip.isass");
        Set<String> packages = new LinkedHashSet<>();
        Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(packages::add);
        return packages;
    }

    private ClassPathScanningCandidateComponentProvider scanner() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment) {
                    @Override
                    protected boolean isCandidateComponent(
                            org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definition) {
                        return definition.getMetadata().isIndependent();
                    }
                };
        scanner.addIncludeFilter(new AnnotationTypeFilter(EntrypointInfo.class));
        return scanner;
    }
}
