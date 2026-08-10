// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.exception;

import cn.hutool.core.exceptions.ValidateException;
import org.junit.jupiter.api.Test;
import vip.isass.framework.common.exception.code.StatusMessageEnum;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.DateTimeException;

import static org.assertj.core.api.Assertions.assertThat;

class BuildInCoreExceptionMappingTest {

    private final BuildInCoreExceptionMapping mapping = new BuildInCoreExceptionMapping();

    @Test
    void mapsIllegalArgumentException() {
        Exception e = new IllegalArgumentException("参数非法");
        assertThat(mapping.getStatusCode(e)).isEqualTo(StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR);
        assertThat(mapping.parseExceptionMessage(e)).isEqualTo("参数非法");
    }

    @Test
    void mapsAbsentExceptionAsNotFound() {
        Exception e = new AbsentException("记录不存在");
        assertThat(mapping.getStatusCode(e)).isEqualTo(StatusMessageEnum.ABSENT);
    }

    @Test
    void mapsAlreadyPresentException() {
        Exception e = new AlreadyPresentException("记录已存在");
        assertThat(mapping.getStatusCode(e)).isEqualTo(StatusMessageEnum.ALREADY_PRESENT);
    }

    @Test
    void mapsUnsupportedOperationException() {
        Exception e = new UnsupportedOperationException("不支持该操作");
        assertThat(mapping.getStatusCode(e)).isEqualTo(StatusMessageEnum.UN_SUPPORT_OPERATION);
    }

    @Test
    void mapsValidateExceptionAsIllegalArgument() {
        Exception e = new ValidateException("校验失败");
        assertThat(mapping.getStatusCode(e)).isEqualTo(StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR);
    }

    @Test
    void mapsIOException() {
        Exception e = new IOException("IO 错误");
        assertThat(mapping.getStatusCode(e)).isEqualTo(StatusMessageEnum.IO_ERROR);
    }

    @Test
    void mapsFileNotFoundException() {
        Exception e = new FileNotFoundException("文件未找到");
        assertThat(mapping.getStatusCode(e)).isEqualTo(StatusMessageEnum.FILE_NOT_FOUND);
    }

    @Test
    void mapsDateTimeException() {
        Exception e = new DateTimeException("日期格式错误");
        assertThat(mapping.getStatusCode(e)).isEqualTo(StatusMessageEnum.DATE_TIME_ERROR);
    }

    @Test
    void mapsUnknownExceptionToNull() {
        Exception e = new RuntimeException("未知异常");
        assertThat(mapping.getStatusCode(e)).isNull();
    }

    @Test
    void parseMessageAppliesStatusMessageFormatting() {
        assertThat(mapping.parseMessage(
                new IllegalArgumentException("原始"),
                StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR))
                .isEqualTo("参数错误:原始");
    }
}
