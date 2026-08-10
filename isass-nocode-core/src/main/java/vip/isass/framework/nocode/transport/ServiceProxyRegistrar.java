// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.transport;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import vip.isass.framework.nocode.contract.ContractResourceLoader;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.service.IService;
import vip.isass.framework.nocode.service.IApplicationService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Registers a remote service proxy only when the generated V4 interface has no
 * local Spring implementation. This is the local > gRPC > HTTP selection's
 * local half; remote priority is handled by {@link TransportResolver}.
 */
public class ServiceProxyRegistrar implements BeanFactoryPostProcessor, BeanClassLoaderAware, PriorityOrdered {

    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("Nocode remote proxy registration requires a BeanDefinitionRegistry");
        }
        for (ServiceContract contract : loadContracts()) {
            registerIfRemote(registry, beanFactory, contract);
        }
    }

    /** Visible for focused registration tests without requiring a packaged contract resource. */
    protected List<ServiceContract> loadContracts() {
        return new ContractResourceLoader(new ObjectMapper(), classLoader)
                .load().stream().flatMap(document -> document.services().stream()).toList();
    }

    private void registerIfRemote(
            BeanDefinitionRegistry registry,
            ConfigurableListableBeanFactory beanFactory,
            ServiceContract contract
    ) {
        Class<?> serviceInterface = loadServiceInterface(contract);
        String[] localBeanNames = beanFactory.getBeanNamesForType(serviceInterface, true, false);
        if (localBeanNames.length > 0) {
            return;
        }
        String beanName = "nocodeRemoteServiceProxy." + contract.serviceInterface();
        if (registry.containsBeanDefinition(beanName)) {
            throw new IllegalStateException("Duplicate remote nocode proxy for "
                    + contract.serviceInterface() + " (" + contract.service() + "/"
                    + contract.entity() + ")");
        }
        RootBeanDefinition definition = new RootBeanDefinition(ServiceProxyFactoryBean.class);
        definition.getPropertyValues().add("serviceInterface", serviceInterface);
        definition.getPropertyValues().add("contract", contract);
        registry.registerBeanDefinition(beanName, definition);
    }

    private Class<?> loadServiceInterface(ServiceContract contract) {
        try {
            Class<?> type = Class.forName(contract.serviceInterface(), false, classLoader);
            if (!type.isInterface() || (!IService.class.isAssignableFrom(type)
                    && !IApplicationService.class.isAssignableFrom(type))) {
                throw new IllegalStateException("Invalid nocode service interface "
                        + contract.serviceInterface() + " for " + contract.service() + "/"
                        + contract.entity() + ": it must be an IService or IApplicationService interface");
            }
            return type;
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Cannot load nocode service interface "
                    + contract.serviceInterface() + " for " + contract.service() + "/"
                    + contract.entity() + "; add the matching V4 API dependency", exception);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
