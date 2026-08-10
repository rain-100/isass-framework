// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import vip.isass.framework.common.exception.code.StatusMessageEnum;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class BuildInWebExceptionMappingTest {

    @Test
    void mapsBindExceptionToIllegalArgumentMessage() {
        BindException exception = bindInvalidForm();
        BuildInWebExceptionMapping mapping = new BuildInWebExceptionMapping();

        assertThat(mapping.getStatusCode(exception)).isEqualTo(StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR);
        assertThat(mapping.parseExceptionMessage(exception))
                .isEqualTo("name: 不能为空");
        assertThat(mapping.parseMessage(exception, StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR))
                .isEqualTo("参数错误:name: 不能为空");
    }

    @Test
    void mapsMethodArgumentNotValidExceptionToFieldMessage() throws NoSuchMethodException {
        MethodArgumentNotValidException exception = methodArgumentNotValidException();
        BuildInWebExceptionMapping mapping = new BuildInWebExceptionMapping();

        assertThat(mapping.getStatusCode(exception)).isEqualTo(StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR);
        assertThat(mapping.parseExceptionMessage(exception))
                .isEqualTo("name: 不能为空");
        assertThat(mapping.parseMessage(exception, StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR))
                .isEqualTo("参数错误:name: 不能为空");
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

    private MethodArgumentNotValidException methodArgumentNotValidException() throws NoSuchMethodException {
        Method method = Controller.class.getDeclaredMethod("save", Form.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BindException bindException = bindInvalidForm();
        return new MethodArgumentNotValidException(methodParameter, bindException.getBindingResult());
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

    static class Controller {
        void save(@Valid Form form) {
        }
    }
}
