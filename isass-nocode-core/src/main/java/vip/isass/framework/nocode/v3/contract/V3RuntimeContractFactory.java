package vip.isass.framework.nocode.v3.contract;

import vip.isass.framework.nocode.v3.V3ServiceRegistry;
import vip.isass.framework.nocode.v3.service.IV3Service;

import java.util.ArrayList;
import java.util.List;

public final class V3RuntimeContractFactory {

    private V3RuntimeContractFactory() {
    }

    public static V3ContractRegistry from(V3ServiceRegistry registry) {
        List<V3ServiceContract> contracts = new ArrayList<>();
        for (IV3Service<?, ?> service : registry.all()) {
            contracts.add(new V3ServiceContract(
                    service.serviceName(),
                    service.entityName(),
                    serviceInterface(service).getName(),
                    service.entityClass().getName(),
                    service.criteriaClass().getName(),
                    V3StandardContractFactory.operations(
                            service.entityClass().getName(),
                            service.criteriaClass().getName())));
        }
        return new V3ContractRegistry(contracts);
    }

    private static Class<?> serviceInterface(IV3Service<?, ?> service) {
        for (Class<?> type : service.getClass().getInterfaces()) {
            if (type != IV3Service.class && IV3Service.class.isAssignableFrom(type)) {
                return type;
            }
        }
        return IV3Service.class;
    }
}
