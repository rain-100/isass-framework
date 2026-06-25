package vip.isass.framework.adapter.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.SmartLifecycle;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;
import vip.isass.framework.adapter.springboot.database.IsassDatabaseSpringBootAutoConfiguration;
import vip.isass.framework.adapter.springboot.converter.IsassSpringConverterAdapter;
import vip.isass.framework.adapter.springboot.destroy.AutoDestroyManager;
import vip.isass.framework.adapter.springboot.mq.IsassMqSpringBootAutoConfiguration;
import vip.isass.framework.common.entity.DbEntityConvert;
import vip.isass.framework.common.exception.BuildInCoreExceptionMapping;
import vip.isass.framework.common.exception.IExceptionMapping;
import vip.isass.framework.common.log.slf4j.LogLevelManager;
import vip.isass.framework.common.selectoption.ISelectOptionService;
import vip.isass.framework.common.selectoption.SelectOption;
import vip.isass.framework.common.selectoption.SelectOptionServiceManager;
import vip.isass.framework.nocode.DictTranslationProviderUtil;
import vip.isass.framework.nocode.IDictTranslationProvider;
import vip.isass.framework.nocode.v2.entity.V2DbEntityConvert;
import vip.isass.framework.common.support.BeanProvider;
import vip.isass.framework.common.support.BeanProviderUtil;
import vip.isass.framework.mq.core.MqManager;
import vip.isass.framework.mq.core.config.DynamicMqProperties;
import vip.isass.framework.mq.core.consumer.MqConsumerAutoConfiguration;
import vip.isass.framework.mq.core.producer.EventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IsassSpringBootAutoConfigurationTest {

    private static final String AUTO_CONFIGURATION_CLASS =
            "vip.isass.framework.adapter.springboot.IsassSpringBootAutoConfiguration";

    private static final String DATABASE_AUTO_CONFIGURATION_CLASS =
            "vip.isass.framework.adapter.springboot.database.IsassDatabaseSpringBootAutoConfiguration";

    private static final String MQ_AUTO_CONFIGURATION_CLASS =
            "vip.isass.framework.adapter.springboot.mq.IsassMqSpringBootAutoConfiguration";

    @Test
    void publishesSpringBootAutoConfigurationImport() {
        List<String> candidates = ImportCandidates
                .load(AutoConfiguration.class, getClass().getClassLoader())
                .getCandidates();

        assertThat(candidates).contains(AUTO_CONFIGURATION_CLASS);
        assertThat(candidates).contains(DATABASE_AUTO_CONFIGURATION_CLASS);
        assertThat(candidates).contains(MQ_AUTO_CONFIGURATION_CLASS);
        assertThat(ClassUtils.isPresent(AUTO_CONFIGURATION_CLASS, getClass().getClassLoader())).isTrue();
        assertThat(ClassUtils.isPresent(DATABASE_AUTO_CONFIGURATION_CLASS, getClass().getClassLoader())).isTrue();
        assertThat(ClassUtils.isPresent(MQ_AUTO_CONFIGURATION_CLASS, getClass().getClassLoader())).isTrue();
    }

    @Test
    void registersBeanProviderForBeanProviderUtilFacade() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassSpringBootAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(BeanProvider.class);
                    assertThat(BeanProviderUtil.isInitialized()).isTrue();
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
    void registersDatabaseExceptionMappingWhenDatabaseCoreIsPresent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IsassSpringBootAutoConfiguration.class,
                        IsassDatabaseSpringBootAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context).hasBean("databaseExceptionMapping");
                    Object mapping = context.getBean("databaseExceptionMapping");
                    assertThat(mapping).isInstanceOf(IExceptionMapping.class);
                    assertThat(mapping.getClass().getName())
                            .isEqualTo("vip.isass.framework.database.core.exception.DatabaseExceptionMapping");
                });
    }

    @Test
    void skipsDatabaseExceptionMappingWhenDatabaseCoreIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("vip.isass.framework.database"))
                .withConfiguration(AutoConfigurations.of(
                        IsassSpringBootAutoConfiguration.class,
                        IsassDatabaseSpringBootAutoConfiguration.class
                ))
                .run(context -> assertThat(context).doesNotHaveBean("databaseExceptionMapping"));
    }

    @Test
    void registersMqBeansWhenMqCoreIsPresent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassMqSpringBootAutoConfiguration.class))
                .withPropertyValues("isass.mq.enabled=false", "isass.mq.primary=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(DynamicMqProperties.class);
                    assertThat(context).hasSingleBean(MqManager.class);
                    assertThat(context).hasSingleBean(EventPublisher.class);
                    assertThat(context).hasSingleBean(MqConsumerAutoConfiguration.class);
                    assertThat(context).hasBean("mqManagerLifecycle");
                    assertThat(context).hasBean("eventPublisherLifecycle");
                    assertThat(context).hasBean("mqConsumerLifecycle");
                    assertThat(context.getBean(DynamicMqProperties.class).getPrimary()).isEqualTo("test");
                    assertThat(context.getBean("mqManagerLifecycle")).isInstanceOf(SmartLifecycle.class);
                });
    }

    @Test
    void skipsMqBeansWhenMqCoreIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("vip.isass.framework.mq"))
                .withConfiguration(AutoConfigurations.of(IsassMqSpringBootAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean("mqManager"));
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
    void bridgesDictTranslationProviderToCoreHolder() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IsassSpringBootAutoConfiguration.class))
                .withBean("dictTranslationProvider",
                        IDictTranslationProvider.class,
                        () -> (typeCode, optionCode) -> "nocode:" + typeCode + ":" + optionCode)
                .run(context -> {
                    assertThat(context).hasSingleBean(BeanProvider.class);
                    assertThat(context).hasSingleBean(IDictTranslationProvider.class);
                    assertThat(DictTranslationProviderUtil.getProvider()
                            .translate("status", "1"))
                            .isEqualTo("nocode:status:1");
                });
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
