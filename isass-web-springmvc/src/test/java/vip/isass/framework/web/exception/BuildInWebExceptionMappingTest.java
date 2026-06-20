package vip.isass.framework.web.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import vip.isass.framework.common.exception.code.StatusMessageEnum;

import jakarta.validation.constraints.NotBlank;

import static org.assertj.core.api.Assertions.assertThat;

class BuildInWebExceptionMappingTest {

    @Test
    void mapsBindExceptionToIllegalArgumentMessage() {
        BindException exception = bindInvalidForm();
        BuildInWebExceptionMapping mapping = new BuildInWebExceptionMapping();

        assertThat(mapping.getStatusCode(exception)).isEqualTo(StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR);
        assertThat(mapping.parseExceptionMessage(exception))
                .contains("参数错误")
                .contains("name")
                .contains("不能为空");
    }

    private BindException bindInvalidForm() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        DataBinder binder = new DataBinder(new Form());
        binder.setValidator(validator);
        binder.bind(new MutablePropertyValues().add("name", ""));
        binder.validate();
        return new BindException(binder.getBindingResult());
    }

    static class Form {
        @NotBlank
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
