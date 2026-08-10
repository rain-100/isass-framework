// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.map.MapUtil;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import vip.isass.framework.common.exception.IExceptionMapping;
import vip.isass.framework.common.exception.code.IStatusMessage;
import vip.isass.framework.common.exception.code.StatusMessageEnum;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Rain
 */
public class BuildInWebExceptionMapping implements IExceptionMapping {

    private static Map<Class<? extends Exception>, IStatusMessage> exceptionMapping = MapUtil.<Class<? extends Exception>, IStatusMessage>builder()
        .put(HttpRequestMethodNotSupportedException.class, StatusMessageEnum.METHOD_NOT_ALLOWED_405)
        .put(HttpMessageNotReadableException.class, StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR)
        .put(HttpMessageConversionException.class, StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR)
        .put(MissingServletRequestParameterException.class, StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR)
        .put(MethodArgumentNotValidException.class, StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR)
        .put(BindException.class, StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR)
        .build();

    @Override
    public IStatusMessage getStatusCode(Exception exception) {
        Throwable unwrap = ExceptionUtil.unwrap(exception);
        return exceptionMapping.get(unwrap.getClass());
    }

    @Override
    public String parseExceptionMessage(Throwable e) {
        Throwable unwrap = ExceptionUtil.unwrap(e);
        if (unwrap instanceof BindException) {
            return parseBindExceptionMessage((BindException) unwrap);
        }
        return unwrap.getMessage();
    }

    private String parseBindExceptionMessage(BindException e) {
        return e.getAllErrors()
                .stream()
                .map(this::parseObjectErrorMessage)
                .collect(Collectors.joining(", "));
    }

    private String parseObjectErrorMessage(ObjectError error) {
        String name = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
        return name + ": " + error.getDefaultMessage();
    }

}
