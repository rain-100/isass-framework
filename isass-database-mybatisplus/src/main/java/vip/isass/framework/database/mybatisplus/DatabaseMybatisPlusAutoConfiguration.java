// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.database.mybatisplus.config.SqlSessionConfig;
import vip.isass.framework.database.mybatisplus.exception.BuildInDatabaseExceptionMapping;
import vip.isass.framework.database.mybatisplus.json.IPageDeserializer;
import vip.isass.framework.database.mybatisplus.typehandler.BigDecimalArrayTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.BooleanArrayTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.DateArrayTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.IntegerArrayTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.JsonNodeTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.JsonValueTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.LocalDateArrayTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.LocalDateTimeArrayTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.LocalDateTimeTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.LocalTimeArrayTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.LongArrayTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.StringArrayTypeHandler;
import vip.isass.framework.database.mybatisplus.typehandler.StringCollectionTypeHandler;
import vip.isass.framework.database.mybatisplus.util.LongSequenceImpl;
import vip.isass.framework.database.mybatisplus.util.SystemClockImpl;

/**
 * @author Rain
 */
@Import(SqlSessionConfig.class)
public class DatabaseMybatisPlusAutoConfiguration implements InitializingBean {

    @Bean
    @ConditionalOnMissingBean
    public BuildInDatabaseExceptionMapping buildInDatabaseExceptionMapping() {
        return new BuildInDatabaseExceptionMapping();
    }

    @Bean
    @ConditionalOnMissingBean
    public BigDecimalArrayTypeHandler bigDecimalArrayTypeHandler() {
        return new BigDecimalArrayTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public BooleanArrayTypeHandler booleanArrayTypeHandler() {
        return new BooleanArrayTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public DateArrayTypeHandler dateArrayTypeHandler() {
        return new DateArrayTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegerArrayTypeHandler integerArrayTypeHandler() {
        return new IntegerArrayTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonNodeTypeHandler jsonNodeTypeHandler() {
        return new JsonNodeTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonValueTypeHandler jsonValueTypeHandler() {
        return new JsonValueTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalDateArrayTypeHandler localDateArrayTypeHandler() {
        return new LocalDateArrayTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalDateTimeArrayTypeHandler localDateTimeArrayTypeHandler() {
        return new LocalDateTimeArrayTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalDateTimeTypeHandler localDateTimeTypeHandler() {
        return new LocalDateTimeTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalTimeArrayTypeHandler localTimeArrayTypeHandler() {
        return new LocalTimeArrayTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LongArrayTypeHandler longArrayTypeHandler() {
        return new LongArrayTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public StringArrayTypeHandler stringArrayTypeHandler() {
        return new StringArrayTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public StringCollectionTypeHandler stringCollectionTypeHandler() {
        return new StringCollectionTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LongSequenceImpl longSequence() {
        return new LongSequenceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public SystemClockImpl systemClock() {
        return new SystemClockImpl();
    }

    @Override
    public void afterPropertiesSet() {
        JsonUtil.simpleModule.addDeserializer(IPage.class, new IPageDeserializer());
        Jackson3TypeHandler.setObjectMapper(JsonUtil.DEFAULT_INSTANCE);
        // JsonUtil.DEFAULT_INSTANCE.registerModule(new PageModule());
        // JsonUtil.NOT_NULL_INSTANCE.registerModule(new PageModule());
    }
}
