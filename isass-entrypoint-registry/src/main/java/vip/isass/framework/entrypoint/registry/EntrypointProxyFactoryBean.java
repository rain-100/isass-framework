// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import vip.isass.framework.entrypoint.IEntrypoint;

import java.lang.reflect.Proxy;

public final class EntrypointProxyFactoryBean implements FactoryBean<IEntrypoint>, BeanFactoryAware {

    private final Class<? extends IEntrypoint> serviceInterface;
    private ListableBeanFactory beanFactory;

    public EntrypointProxyFactoryBean(Class<? extends IEntrypoint> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    @Override
    public IEntrypoint getObject() {
        return (IEntrypoint) Proxy.newProxyInstance(serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface, RemoteEntrypointProxy.class},
                new EntrypointRemoteInvocationHandler(serviceInterface, beanFactory));
    }

    @Override
    public Class<?> getObjectType() {
        return serviceInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = (ListableBeanFactory) beanFactory;
    }
}
