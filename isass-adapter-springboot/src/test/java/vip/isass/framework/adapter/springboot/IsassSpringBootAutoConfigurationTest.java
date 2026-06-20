package vip.isass.framework.adapter.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;
import vip.isass.framework.adapter.springboot.converter.IsassSpringConverterAdapter;
import vip.isass.framework.adapter.springboot.destroy.AutoDestroyManager;
import vip.isass.framework.common.entity.DbEntityConvert;
import vip.isass.framework.common.exception.BuildInCoreExceptionMapping;
import vip.isass.framework.common.log.slf4j.LogLevelManager;
import vip.isass.framework.common.selectoption.ISelectOptionService;
import vip.isass.framework.common.selectoption.SelectOption;
import vip.isass.framework.common.selectoption.SelectOptionServiceManager;
import vip.isass.framework.common.structure.entity.V2DbEntityConvert;
import vip.isass.framework.common.support.BeanProvider;
import vip.isass.framework.common.support.SpringContextUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IsassSpringBootAutoConfigurationTest {

    private static final String AUTO_CONFIGURATION_CLASS =
            "vip.isass.framework.adapter.springboot.IsassSpringBootAutoConfiguration";

    @Test
    void publishesSpringBootAutoConfigurationImport() {
        List<String> candidates = ImportCandidates
                .load(AutoConfiguration.class, getClass().getClassLoader())
                .getCandidates();

        assertThat(candidates).contains(AUTO_CONFIGURATION_CLASS);
        assertThat(ClassUtils.isPresent(AUTO_CONFIGURATION_CLASS, getClass().getClassLoader())).isTrue();
    }

    @Test
    void registersBeanProviderForSpringContextUtilFacade() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassSpringBootAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(BeanProvider.class);
                    assertThat(SpringContextUtil.isInitialized()).isTrue();
                });
    }

    @Test
    void registersSelectOptionServiceManagerWithoutServices() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassSpringBootAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(SelectOptionServiceManager.class);
                    assertThat(context.getBean(SelectOptionServiceManager.class).getSelectOptionServices()).isEmpty();
                });
    }

    @Test
    void registersSelectOptionServicesIntoManager() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassSpringBootAutoConfiguration.class))
                .withBean(TestSelectOptionService.class)
                .run(context -> {
                    SelectOptionServiceManager manager = context.getBean(SelectOptionServiceManager.class);

                    assertThat(manager.getSelectOptionServices())
                            .containsEntry("test", context.getBean(TestSelectOptionService.class));
                });
    }

    @Test
    void registersAutoDestroyManager() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassSpringBootAutoConfiguration.class))
                .run(context -> assertThat(context).hasSingleBean(AutoDestroyManager.class));
    }

    @Test
    void registersCoreExceptionMapping() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassSpringBootAutoConfiguration.class))
                .run(context -> assertThat(context).hasSingleBean(BuildInCoreExceptionMapping.class));
    }

    @Test
    void registersSpringConverterAdapter() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassSpringBootAutoConfiguration.class))
                .run(context -> assertThat(context).hasSingleBean(IsassSpringConverterAdapter.class));
    }

    @Test
    void registersLogLevelManager() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassSpringBootAutoConfiguration.class))
                .run(context -> assertThat(context).hasSingleBean(LogLevelManager.class));
    }

    @Test
    void registersDbEntityConvertersWithConfiguredPackageName() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassSpringBootAutoConfiguration.class))
                .withPropertyValues("info.package=vip.isass.test")
                .run(context -> {
                    assertThat(context).hasSingleBean(DbEntityConvert.class);
                    assertThat(context).hasSingleBean(V2DbEntityConvert.class);
                    assertThat(ReflectionTestUtils.getField(DbEntityConvert.class, "packageName"))
                            .isEqualTo("vip.isass.test");
                    assertThat(ReflectionTestUtils.getField(V2DbEntityConvert.class, "packageName"))
                            .isEqualTo("vip.isass.test");
                });
    }

    @Test
    void doesNotBringFeatureModuleAutoConfigurations() {
        ClassLoader classLoader = getClass().getClassLoader();

        assertThat(ClassUtils.isPresent("vip.isass.framework.database.core.DatabaseAutoConfiguration", classLoader)).isFalse();
        assertThat(ClassUtils.isPresent("vip.isass.framework.mq.core.MqAutoConfiguration", classLoader)).isFalse();
        assertThat(ClassUtils.isPresent("vip.isass.framework.net.core.NetCoreAutoConfiguration", classLoader)).isFalse();
        assertThat(ClassUtils.isPresent("vip.isass.framework.web.WebAutoConfiguration", classLoader)).isFalse();
        assertThat(ClassUtils.isPresent("vip.isass.framework.web.security.WebSecurityAutoConfiguration", classLoader)).isFalse();
    }

    static class TestSelectOptionService implements ISelectOptionService<String> {

        @Override
        public String getKey() {
            return "test";
        }

        @Override
        public List<SelectOption<String>> getSelectOptions() {
            return List.of(new SelectOption<String>().setName("测试").setValue("test"));
        }
    }
}
