package vip.isass.framework.nocode.contract;

import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.service.IService;

import java.util.ArrayList;
import java.util.List;

public final class RuntimeContractFactory {

    private RuntimeContractFactory() {
    }

    public static ContractRegistry from(ServiceRegistry registry) {
        List<ServiceContract> contracts = new ArrayList<>();
        for (IService<?, ?> service : registry.all()) {
            contracts.add(new ServiceContract(
                    service.serviceName(),
                    service.entityName(),
                    serviceInterface(service).getName(),
                    service.entityClass().getName(),
                    service.criteriaClass().getName(),
                    StandardContractFactory.operations(
                            service.entityClass().getName(),
                            service.criteriaClass().getName())));
        }
        return new ContractRegistry(contracts);
    }

    private static Class<?> serviceInterface(IService<?, ?> service) {
        for (Class<?> type : service.getClass().getInterfaces()) {
            if (type != IService.class && IService.class.isAssignableFrom(type)) {
                return type;
            }
        }
        return IService.class;
    }
}
