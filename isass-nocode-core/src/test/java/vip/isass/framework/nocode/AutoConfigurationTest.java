package vip.isass.framework.nocode;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.repository.IRepository;
import vip.isass.framework.nocode.service.ILocalService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class AutoConfigurationTest {

    @Test
    void serviceRegistryUsesOnlyLocalServices() {
        LocalService localService = new LocalService();

        ServiceRegistry registry = new AutoConfiguration()
                .ServiceRegistry(List.of(localService));

        assertSame(localService, registry.require(localService.service(), localService.entity()));
    }

    static final class LocalService implements ILocalService<Entity, Criteria> {

        @Override
        public IRepository<Entity, Criteria> getRepository() {
            throw new UnsupportedOperationException();
        }
    }

    static final class Entity implements IEntity<Entity> {

        @Override
        public Entity randomEntity() {
            return this;
        }
    }

    static final class Criteria implements ICriteria<Entity, Criteria> {
    }
}
