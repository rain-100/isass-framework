package vip.isass.framework.adapter.springboot.support;

import cn.hutool.core.map.MapUtil;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import vip.isass.framework.common.support.BeanProvider;
import vip.isass.framework.common.support.SpringContextUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SpringBeanProvider implements BeanProvider {

    private final ConfigurableApplicationContext applicationContext;

    public SpringBeanProvider(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object getContext() {
        return applicationContext;
    }

    @Override
    public <T> T addBean(Class<T> beanClass) {
        DefaultListableBeanFactory beanFactory = getBeanFactory();
        String beanName = SpringContextUtil.getBeanNameByBeanType(beanClass);
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
        beanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
        return applicationContext.getBean(beanName, beanClass);
    }

    @Override
    public Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return applicationContext.getBean(name, requiredType);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        return applicationContext.getBean(requiredType);
    }

    @Override
    public <T> Collection<T> getBeans(Class<T> requiredType) {
        Map<String, T> beans = applicationContext.getBeansOfType(requiredType);
        if (MapUtil.isEmpty(beans)) {
            return Collections.emptyList();
        }
        return beans.values();
    }

    @Override
    public <T, P> T getBean(Class<T> requiredType, Class<P> type) {
        Map<String, T> beans = applicationContext.getBeansOfType(requiredType);
        if (MapUtil.isEmpty(beans)) {
            return null;
        }

        for (T bean : beans.values()) {
            ResolvableType resolvableType = ResolvableType.forClass(bean.getClass());
            Class<?> resolve = resolvableType.getSuperType().getGeneric(0).resolve();
            if (type.equals(resolve)) {
                return bean;
            }
        }
        return null;
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... objects) {
        return applicationContext.getBean(requiredType, objects);
    }

    @Override
    public void unRegistryBean(String beanName) {
        DefaultListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory.containsBeanDefinition(beanName)) {
            beanFactory.destroySingleton(beanName);
            beanFactory.removeBeanDefinition(beanName);
        }
    }

    @Override
    public void unRegistryBean(Class<?> beanClass) {
        String[] beanNames = applicationContext.getBeanNamesForType(beanClass);
        for (String beanName : beanNames) {
            unRegistryBean(beanName);
        }
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        return applicationContext.getBeanNamesForType(type);
    }

    private DefaultListableBeanFactory getBeanFactory() {
        return (DefaultListableBeanFactory) applicationContext.getBeanFactory();
    }
}
