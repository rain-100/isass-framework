// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.transport;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.NonNull;
import vip.isass.framework.nocode.contract.ServiceContract;

import java.util.ArrayList;
import java.util.List;

/** Creates one typed remote proxy for a generated V4 service interface. */
public class ServiceProxyFactoryBean implements FactoryBean<Object>, BeanFactoryAware, InitializingBean {

    private Class<?> serviceInterface;
    private ServiceContract contract;
    private ListableBeanFactory beanFactory;
    private Object proxy;

    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public void setContract(ServiceContract contract) {
        this.contract = contract;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        if (!(beanFactory instanceof ListableBeanFactory listableBeanFactory)) {
            throw new IllegalStateException("Remote service proxies require a ListableBeanFactory");
        }
        this.beanFactory = listableBeanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        if (serviceInterface == null || contract == null || beanFactory == null) {
            throw new IllegalStateException("Invalid remote service proxy definition");
        }
        proxy = new ServiceProxyFactory(new TransportResolver()).create(
                serviceInterface, contract, this::remoteTransports);
    }

    private List<InvocationTransport> remoteTransports() {
        List<InvocationTransport> transports = new ArrayList<>();
        beanFactory.getBeansOfType(RemoteTransportProvider.class).values().forEach(provider ->
                transports.addAll(provider.transports(contract)));
        return List.copyOf(transports);
    }

    @Override
    public Object getObject() {
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return serviceInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
