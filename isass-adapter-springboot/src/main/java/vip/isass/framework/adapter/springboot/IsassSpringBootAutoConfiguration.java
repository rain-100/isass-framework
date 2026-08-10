// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.adapter.springboot;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import vip.isass.framework.adapter.springboot.converter.IsassSpringConverterAdapter;
import vip.isass.framework.adapter.springboot.destroy.AutoDestroyManager;
import vip.isass.framework.adapter.springboot.log.SpringBootLogLevelManager;
import vip.isass.framework.adapter.springboot.support.SpringBeanProvider;
import vip.isass.framework.common.exception.BuildInCoreExceptionMapping;
import vip.isass.framework.common.log.slf4j.LogLevelManager;
import vip.isass.framework.common.log.slf4j.LogUtil;
import vip.isass.framework.common.security.CurrentPrincipalService;
import vip.isass.framework.common.security.CurrentPrincipalUtil;
import vip.isass.framework.common.selectoption.ISelectOptionService;
import vip.isass.framework.common.selectoption.SelectOptionServiceManager;
import vip.isass.framework.common.sequence.Sequence;
import vip.isass.framework.common.sequence.impl.LongSequence;
import vip.isass.framework.common.support.BeanProvider;
import vip.isass.framework.common.support.BeanProviderUtil;
import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.ISystemClock;
import vip.isass.framework.common.support.IsassServiceLoader;
import vip.isass.framework.common.support.SystemClock;
import vip.isass.framework.nocode.IDictTranslationProvider;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
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
    public BuildInCoreExceptionMapping buildInCoreExceptionMapping() {
        return new BuildInCoreExceptionMapping();
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanProvider beanProvider(ConfigurableApplicationContext applicationContext,
                                     ObjectProvider<CurrentPrincipalService> currentPrincipalServiceProvider,
                                     ObjectProvider<ISystemClock> systemClockProvider,
                                     ObjectProvider<Sequence<?>> sequenceProvider,
                                     ObjectProvider<IDictTranslationProvider> nocodeDictTranslationProvider) {
        BeanProvider beanProvider = new SpringBeanProvider(applicationContext);
        BeanProviderUtil.setBeanProvider(beanProvider);
        CurrentPrincipalUtil.setCurrentPrincipalServiceProvider(currentPrincipalServiceProvider::getIfAvailable);
        SystemClock.setSystemClockProvider(systemClockProvider::getIfAvailable);
        LongSequence.setSequenceProvider(() -> getLongSequence(sequenceProvider));
        vip.isass.framework.nocode.DictTranslationProviderUtil.setProviderSupplier(
                nocodeDictTranslationProvider::getIfAvailable);
        return beanProvider;
    }

    @SuppressWarnings("unchecked")
    private Sequence<Long> getLongSequence(ObjectProvider<Sequence<?>> sequenceProvider) {
        return (Sequence<Long>) sequenceProvider.orderedStream()
                .filter(sequence -> sequence.support(Long.class))
                .findFirst().orElse(null);
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
        List<Converter<?, ?>> spiConverters = new ArrayList<>();
        for (Converter<?, ?> converter : IsassServiceLoader.load(Converter.class)) {
            spiConverters.add(converter);
        }
        List<Converter<?, ?>> converters = IsassServiceLoader.mergeByClass(
                customConverters.orderedStream().toList(),
                spiConverters
        );
        return new IsassSpringConverterAdapter(converters);
    }
}
