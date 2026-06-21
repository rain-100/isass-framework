package vip.isass.framework.adapter.springboot;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import vip.isass.framework.adapter.springboot.converter.IsassSpringConverterAdapter;
import vip.isass.framework.adapter.springboot.destroy.AutoDestroyManager;
import vip.isass.framework.adapter.springboot.log.SpringBootLogLevelManager;
import vip.isass.framework.adapter.springboot.support.SpringBeanProvider;
import vip.isass.framework.common.converter.StringToMapConverter;
import vip.isass.framework.common.converter.datatime.CollectionToQueryStringConverter;
import vip.isass.framework.common.converter.datatime.DateToStringTimestampConverter;
import vip.isass.framework.common.converter.datatime.LocalDateTimeToStringTimestampConverter;
import vip.isass.framework.common.converter.datatime.MapToJsonConverter;
import vip.isass.framework.common.converter.datatime.StringDateToMillisConverter;
import vip.isass.framework.common.converter.datatime.StringToClassConverter;
import vip.isass.framework.common.converter.datatime.StringToLocalDateConverter;
import vip.isass.framework.common.converter.datatime.StringToLocalDateTimeConverter;
import vip.isass.framework.common.converter.datatime.StringToLocalTimeConverter;
import vip.isass.framework.common.entity.DbEntityConvert;
import vip.isass.framework.common.exception.BuildInCoreExceptionMapping;
import vip.isass.framework.common.login.LoginUserService;
import vip.isass.framework.common.login.LoginUserUtil;
import vip.isass.framework.common.log.slf4j.LogLevelManager;
import vip.isass.framework.common.log.slf4j.LogUtil;
import vip.isass.framework.common.selectoption.ISelectOptionService;
import vip.isass.framework.common.selectoption.SelectOptionServiceManager;
import vip.isass.framework.common.sequence.Sequence;
import vip.isass.framework.common.sequence.impl.LongSequence;
import vip.isass.framework.nocode.v2.converter.StringToV2WhereConditionConverter;
import vip.isass.framework.nocode.v2.entity.V2DbEntityConvert;
import vip.isass.framework.common.support.BeanProvider;
import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.BeanProviderUtil;
import vip.isass.framework.common.support.ISystemClock;
import vip.isass.framework.common.support.SystemClock;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@ComponentScan(basePackages = "vip.isass.framework.common")
public class IsassSpringBootAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SelectOptionServiceManager selectOptionServiceManager(ObjectProvider<ISelectOptionService<?>> services) {
        return new SelectOptionServiceManager(services.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public AutoDestroyManager autoDestroyManager() {
        return new AutoDestroyManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public DbEntityConvert dbEntityConvert(@Value("${info.package:}") String packageName) {
        DbEntityConvert converter = new DbEntityConvert();
        converter.setPackageName(packageName);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    public V2DbEntityConvert v2DbEntityConvert(@Value("${info.package:}") String packageName) {
        V2DbEntityConvert converter = new V2DbEntityConvert();
        converter.setPackageName(packageName);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    public BuildInCoreExceptionMapping buildInCoreExceptionMapping() {
        return new BuildInCoreExceptionMapping();
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanProvider beanProvider(ConfigurableApplicationContext applicationContext,
                                     ObjectProvider<LoginUserService> loginUserServiceProvider,
                                     ObjectProvider<ISystemClock> systemClockProvider,
                                     ObjectProvider<Sequence<?>> sequenceProvider,
                                     ObjectProvider<vip.isass.framework.common.structure.IDictTranslationProvider> commonDictTranslationProvider,
                                     ObjectProvider<vip.isass.framework.nocode.IDictTranslationProvider> nocodeDictTranslationProvider) {
        BeanProvider beanProvider = new SpringBeanProvider(applicationContext);
        BeanProviderUtil.setBeanProvider(beanProvider);
        LoginUserUtil.setLoginUserServiceProvider(loginUserServiceProvider::getIfAvailable);
        SystemClock.setSystemClockProvider(systemClockProvider::getIfAvailable);
        LongSequence.setSequenceProvider(() -> getLongSequence(sequenceProvider));
        vip.isass.framework.common.structure.DictTranslationProviderUtil.setProviderSupplier(
                commonDictTranslationProvider::getIfAvailable);
        vip.isass.framework.nocode.DictTranslationProviderUtil.setProviderSupplier(
                nocodeDictTranslationProvider::getIfAvailable);
        return beanProvider;
    }

    @SuppressWarnings("unchecked")
    private Sequence<Long> getLongSequence(ObjectProvider<Sequence<?>> sequenceProvider) {
        return sequenceProvider.orderedStream()
            .filter(sequence -> sequence.support(Long.class))
            .findFirst()
            .map(sequence -> (Sequence<Long>) sequence)
            .orElse(null);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogLevelManager logLevelManager() {
        LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
        LogLevelManager logLevelManager = new SpringBootLogLevelManager(loggingSystem);
        LogUtil.setLogLevelManager(logLevelManager);
        return logLevelManager;
    }

    @Bean
    @ConditionalOnMissingBean
    public IsassSpringConverterAdapter isassSpringConverterAdapter(ObjectProvider<Converter<?, ?>> customConverters) {
        List<Converter<?, ?>> converters = new ArrayList<>(customConverters.orderedStream().toList());
        for (Converter<?, ?> defaultConverter : defaultConverters()) {
            boolean exists = converters.stream()
                .anyMatch(converter -> converter.getClass().equals(defaultConverter.getClass()));
            if (!exists) {
                converters.add(defaultConverter);
            }
        }
        return new IsassSpringConverterAdapter(converters);
    }

    private List<Converter<?, ?>> defaultConverters() {
        return List.of(
            new StringToLocalDateConverter(),
            new DateToStringTimestampConverter(),
            new StringToV2WhereConditionConverter(),
            new LocalDateTimeToStringTimestampConverter(),
            new StringToClassConverter(),
            new StringToLocalDateTimeConverter(),
            new MapToJsonConverter(),
            new StringToMapConverter(),
            new StringDateToMillisConverter(),
            new StringToLocalTimeConverter(),
            new CollectionToQueryStringConverter()
        );
    }
}
