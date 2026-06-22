package vip.isass.framework.web.exception;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.web.Resp;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionAdviceTest {

    @Test
    void createRespByExceptionShouldKeepDetailMessageWhenUnifiedMessageIsReturned() {
        ExceptionAdvice advice = new ExceptionAdvice(false, "系统繁忙，请稍后重试");

        Resp<?> resp = advice.createRespByException(new IllegalStateException("secret detail"));

        assertThat(resp.getMessage()).contains("系统繁忙，请稍后重试");
        assertThat(resp.getMessage()).doesNotContain("secret detail");
        assertThat(resp.getDetailMessage()).contains("IllegalStateException: secret detail");
    }
}
