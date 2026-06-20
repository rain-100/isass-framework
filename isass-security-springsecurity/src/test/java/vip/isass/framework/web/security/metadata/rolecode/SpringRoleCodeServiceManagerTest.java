package vip.isass.framework.web.security.metadata.rolecode;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vip.isass.framework.common.web.security.metadata.rolecode.IRoleCodeService;
import vip.isass.framework.common.web.security.metadata.rolecode.RoleCodeServiceManager;
import vip.isass.framework.common.web.security.metadata.rolecode.UriRoleCodesReq;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringRoleCodeServiceManagerTest {

    @Test
    void registersPrimaryRoleCodeServiceManagerInSecurityModule() {
        new ApplicationContextRunner()
                .withBean(TestRoleCodeService.class)
                .withBean(SpringRoleCodeServiceManager.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(RoleCodeServiceManager.class);

                    IRoleCodeService service = context.getBean(IRoleCodeService.class);
                    assertThat(service).isInstanceOf(SpringRoleCodeServiceManager.class);
                    assertThat(service.findRoleCodesByUserId("u1")).containsExactly("role:user");
                    assertThat(service.findRoleCodesByUri(new UriRoleCodesReq().setUri("/demo"))).containsExactly("role:uri");
                });
    }

    static class TestRoleCodeService implements IRoleCodeService {

        @Override
        public Collection<String> findRoleCodesByUri(UriRoleCodesReq roleCodesReq) {
            return List.of("role:uri");
        }

        @Override
        public void setRoleCodesByUserIdCache(String userId, Collection<String> roleCodes) {
        }

        @Override
        public void setRoleCodesByUriCache(String uri, Collection<String> roleCodes) {
        }

        @Override
        public Collection<String> findRoleCodesByUserId(String userId) {
            return List.of("role:user");
        }
    }
}
