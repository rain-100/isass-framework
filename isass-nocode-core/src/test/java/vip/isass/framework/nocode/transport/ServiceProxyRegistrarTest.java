// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.transport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.service.IService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceProxyRegistrarTest {

    @Test
    void registersTypedRemoteProxyWhenNoLocalServiceExists() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ServiceContract contract = new ServiceContract(
                "asset-service", "icon", ExampleService.class.getName(),
                "sample.Icon", "sample.IconCriteria", List.of());

        new ServiceProxyRegistrar() {
            @Override
            protected List<ServiceContract> loadContracts() {
                return List.of(contract);
            }
        }.postProcessBeanFactory(beanFactory);

        assertTrue(beanFactory.containsBeanDefinition(
                "nocodeRemoteServiceProxy." + ExampleService.class.getName()));
        assertInstanceOf(ExampleService.class, beanFactory.getBean(ExampleService.class));
    }

    @Test
    void keepsLocalServiceAsTheOnlyCandidate() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerBeanDefinition("localExampleService",
                new RootBeanDefinition(LocalExampleService.class));
        ServiceContract contract = new ServiceContract(
                "asset-service", "icon", ExampleService.class.getName(),
                "sample.Icon", "sample.IconCriteria", List.of());

        new ServiceProxyRegistrar() {
            @Override
            protected List<ServiceContract> loadContracts() {
                return List.of(contract);
            }
        }.postProcessBeanFactory(beanFactory);

        assertFalse(beanFactory.containsBeanDefinition(
                "nocodeRemoteServiceProxy." + ExampleService.class.getName()));
    }

    interface ExampleService extends IService<ExampleEntity, ExampleCriteria> {
    }

    abstract static class LocalExampleService implements ExampleService {
    }

    static final class ExampleEntity implements IEntity<ExampleEntity> {

        @Override
        public ExampleEntity randomEntity() {
            return this;
        }
    }

    static final class ExampleCriteria implements ICriteria<ExampleEntity, ExampleCriteria> {
    }
}
