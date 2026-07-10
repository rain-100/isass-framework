package vip.isass.framework.web.exception;

import cn.hutool.core.exceptions.ValidateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import vip.isass.framework.common.exception.AbsentException;
import vip.isass.framework.common.exception.AlreadyPresentException;
import vip.isass.framework.common.exception.IExceptionMapping;
import vip.isass.framework.common.exception.UnifiedException;
import vip.isass.framework.common.exception.code.StatusMessageEnum;
import vip.isass.framework.common.web.Resp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.DateTimeException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionAdviceTest {

    // ==================== UnifiedException ====================

    @Test
    void unifiedExceptionWithStatusUsesThatStatus() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(
                new UnifiedException(StatusMessageEnum.ABSENT, "记录不存在"));

        assertThat(resp.getSuccess()).isFalse();
        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.ABSENT.getStatus());
        assertThat(resp.getMessage()).isEqualTo("记录不存在");
    }

    @Test
    void unifiedExceptionWithoutStatusAndWithCauseDelegatesToCauseMapping() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(
                new UnifiedException(new IllegalArgumentException("参数非法")));

        assertThat(resp.getSuccess()).isFalse();
        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR.getStatus());
    }

    @Test
    void unifiedExceptionWithoutStatusAndWithoutCauseFallsBackToUndefined() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new UnifiedException((Integer) null));

        assertThat(resp.getSuccess()).isFalse();
        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.UNDEFINED.getStatus());
    }

    @Test
    void unifiedExceptionWithStatusAndMessageUsesProvidedMessage() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(
                new UnifiedException(StatusMessageEnum.FAIL, "自定义失败消息"));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.FAIL.getStatus());
        assertThat(resp.getMessage()).isEqualTo("自定义失败消息");
    }

    // ==================== Core Exception Mappings ====================

    @Test
    void mapsIllegalArgumentException() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new IllegalArgumentException("参数非法"));

        assertThat(resp.getSuccess()).isFalse();
        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR.getStatus());
        assertThat(resp.getMessage()).contains("参数非法");
    }

    @Test
    void mapsValidateException() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new ValidateException("校验失败"));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR.getStatus());
        assertThat(resp.getMessage()).contains("校验失败");
    }

    @Test
    void mapsAbsentException() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new AbsentException("记录不存在"));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.ABSENT.getStatus());
        assertThat(resp.getMessage()).contains("记录不存在");
    }

    @Test
    void mapsAlreadyPresentException() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new AlreadyPresentException("记录已存在"));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.ALREADY_PRESENT.getStatus());
        assertThat(resp.getMessage()).contains("记录已存在");
    }

    @Test
    void mapsUnsupportedOperationException() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new UnsupportedOperationException("不支持"));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.UN_SUPPORT_OPERATION.getStatus());
    }

    @Test
    void mapsIOException() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new IOException("IO 错误"));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.IO_ERROR.getStatus());
        assertThat(resp.getMessage()).contains("IO 错误");
    }

    @Test
    void mapsFileNotFoundException() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new FileNotFoundException("文件未找到"));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.FILE_NOT_FOUND.getStatus());
        assertThat(resp.getMessage()).contains("文件未找到");
    }

    @Test
    void mapsDateTimeException() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new DateTimeException("日期格式错误"));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.DATE_TIME_ERROR.getStatus());
        assertThat(resp.getMessage()).contains("日期格式错误");
    }

    // ==================== Unmapped Exception ====================

    @Test
    void unmappedRuntimeExceptionFallsBackToUndefined() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new RuntimeException("未知异常"));

        assertThat(resp.getSuccess()).isFalse();
        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.UNDEFINED.getStatus());
    }

    @Test
    void faviconNoResourceReturnsPlain404WithoutRespBody() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Object response = advice.exceptionHandler(
                new NoResourceFoundException(HttpMethod.GET, "favicon.ico", "/favicon.ico"));

        assertThat(response).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) response;
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entity.getBody()).isNull();
    }

    @Test
    void configuredNoResourceUrlReturnsPlain404WithoutRespBody() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙", List.of("/robots.txt"));

        Object response = advice.exceptionHandler(
                new NoResourceFoundException(HttpMethod.GET, "robots.txt", "/robots.txt"));

        assertThat(response).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) response;
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entity.getBody()).isNull();
    }

    @Test
    void configuredNoResourceUrlCanBindFromEnvironmentProperty() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("isass.framework.web.exception.silent-not-found-urls", "/robots.txt,/apple-touch-icon.png");
        @SuppressWarnings("unchecked")
        ObjectProvider<IExceptionMapping> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenReturn(Stream.empty());
        ExceptionAdvice advice = new ExceptionAdvice(provider, true, "系统繁忙", environment);

        Object response = advice.exceptionHandler(
                new NoResourceFoundException(HttpMethod.GET, "apple-touch-icon.png", "/apple-touch-icon.png"));

        assertThat(response).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) response;
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entity.getBody()).isNull();
    }

    @Test
    void otherNoResourceStillUsesUnifiedErrorBody() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Object response = advice.exceptionHandler(
                new NoResourceFoundException(HttpMethod.GET, "missing.js", "/missing.js"));

        assertThat(response).isInstanceOf(Resp.class);
        Resp<?> resp = (Resp<?>) response;
        assertThat(resp.getSuccess()).isFalse();
        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.UNDEFINED.getStatus());
    }

    @Test
    void unmappedExceptionWithShowDetailIncludesMessage() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(new IllegalStateException("内部状态错误"));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.UNDEFINED.getStatus());
        assertThat(resp.getMessage()).contains("内部状态错误");
        assertThat(resp.getDetailMessage()).contains("IllegalStateException");
    }

    // ==================== showDetailError toggle ====================

    @Test
    void showDetailFalseReturnsUnifiedMessageForUnmapped() {
        ExceptionAdvice advice = new ExceptionAdvice(false, "系统繁忙，请稍后重试");

        Resp<?> resp = advice.createRespByException(new RuntimeException("secret detail"));

        assertThat(resp.getMessage()).contains("系统繁忙，请稍后重试");
        assertThat(resp.getMessage()).doesNotContain("secret detail");
        assertThat(resp.getDetailMessage()).isNull();
    }

    @Test
    void showDetailTrueReturnsOriginalMessageForUnmapped() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙，请稍后重试");

        Resp<?> resp = advice.createRespByException(new RuntimeException("错误详情"));

        assertThat(resp.getMessage()).contains("错误详情");
    }

    @Test
    void showDetailIncludesConciseCauseChainAndBusinessFrames() {
        Exception cause = new IllegalArgumentException("inner");
        cause.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("org.springframework.Proxy", "call", "Proxy.java", 1),
                new StackTraceElement("vip.isass.demo.Service", "load", "Service.java", 42)
        });
        Exception outer = new RuntimeException("outer", cause);
        outer.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("vip.isass.demo.Controller", "query", "Controller.java", 18)
        });

        Resp<?> resp = new ExceptionAdvice(true, "系统繁忙").createRespByException(outer);

        assertThat(resp.getDetailMessage())
                .contains("RuntimeException: outer")
                .contains("Caused by: IllegalArgumentException: inner")
                .contains("vip.isass.demo.Service.load(Service.java:42)")
                .doesNotContain("org.springframework.Proxy");
        assertThat(resp.getDetailMessage().lines().count()).isLessThanOrEqualTo(30);
        assertThat(resp.getDetailMessage().length()).isLessThanOrEqualTo(8192);
    }

    @Test
    void mappedExceptionBypassesShowDetailToggle() {
        ExceptionAdvice advice = new ExceptionAdvice(false, "系统繁忙，请稍后重试");

        Resp<?> resp = advice.createRespByException(new AbsentException("记录不存在"));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.ABSENT.getStatus());
        assertThat(resp.getMessage()).contains("记录不存在");
    }

    // ==================== UnifiedException wrapping ====================

    @Test
    void unifiedExceptionWrappingMappedCauseDelegatesToMapping() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(
                new UnifiedException(new AbsentException("原始异常")));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.ABSENT.getStatus());
    }

    @Test
    void unifiedExceptionWrappingUnmappedCauseFallsBackToUndefined() {
        ExceptionAdvice advice = new ExceptionAdvice(true, "系统繁忙");

        Resp<?> resp = advice.createRespByException(
                new UnifiedException(new RuntimeException("未知")));

        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.UNDEFINED.getStatus());
    }
}
