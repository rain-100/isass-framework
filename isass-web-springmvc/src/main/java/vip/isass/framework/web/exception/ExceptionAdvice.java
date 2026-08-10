// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import vip.isass.framework.common.exception.IExceptionMapping;
import vip.isass.framework.common.exception.UnifiedException;
import vip.isass.framework.common.exception.code.IStatusMessage;
import vip.isass.framework.common.exception.code.StatusMessageEnum;
import vip.isass.framework.common.sequence.impl.LongSequence;
import vip.isass.framework.common.support.IsassServiceLoader;
import vip.isass.framework.common.web.Resp;

// import javax.annotation.Resource;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 所有异常转换成 Resp
 *
 * @author Rain
 */

@RestControllerAdvice
public class ExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger(ExceptionAdvice.class);

    private final List<IExceptionMapping> exceptionMappings;

    private final boolean showDetailError;

    private final String prodUnifiedMessage;

    private final Set<String> silentNotFoundUrls;

    /**
     * 配置项：是否显示详细错误信息（生产环境建议关闭，开发环境建议开启）
     * 配置项：生产环境统一错误提示语
     */
    @Autowired
    public ExceptionAdvice(ObjectProvider<IExceptionMapping> exceptionMappings,
                           @Value("${isass.web.exception.show-detail-error:true}") boolean showDetailError,
                           @Value("${isass.web.exception.prod-unified-message:系统繁忙，请稍后重试}") String prodUnifiedMessage,
                           Environment environment) {
        this.showDetailError = showDetailError;
        this.prodUnifiedMessage = prodUnifiedMessage;
        this.silentNotFoundUrls = silentNotFoundUrls(loadSilentNotFoundUrls(environment));
        this.exceptionMappings = IsassServiceLoader.mergeByClass(
                exceptionMappings.orderedStream().toList(),
                IsassServiceLoader.load(IExceptionMapping.class)
        );
    }

    ExceptionAdvice(boolean showDetailError, String prodUnifiedMessage) {
        this(showDetailError, prodUnifiedMessage, List.of());
    }

    ExceptionAdvice(boolean showDetailError, String prodUnifiedMessage, List<String> silentNotFoundUrls) {
        this.showDetailError = showDetailError;
        this.prodUnifiedMessage = prodUnifiedMessage;
        this.silentNotFoundUrls = silentNotFoundUrls(silentNotFoundUrls);
        this.exceptionMappings = IsassServiceLoader.load(IExceptionMapping.class);
    }

    /**
     * 处理 controller 抛出的异常
     */
    @ExceptionHandler(Exception.class)
    Object exceptionHandler(Exception e) {
        if (isSilentNotFound(e)) {
            return ResponseEntity.notFound().build();
        }
        if (e instanceof UnifiedException) {
            log.debug(e.getMessage(), e);
        } else if (ExceptionUtil.isCausedBy(e, ClientAbortException.class)) {
            log.debug("http 链接被客户端断开，io操作失败：{}", e.getMessage());
            return null;
        } else {
            log.error(e.getMessage(), e);
        }
        return createRespByException(e);
    }

    private boolean isSilentNotFound(Exception e) {
        if (!(e instanceof NoResourceFoundException noResourceFoundException)) {
            return false;
        }
        return silentNotFoundUrls.contains(normalizeUrl(noResourceFoundException.getResourcePath()));
    }

    private Set<String> silentNotFoundUrls(List<String> configuredUrls) {
        Set<String> urls = new LinkedHashSet<>();
        urls.add("/favicon.ico");
        urls.add("/.well-known/appspecific/com.chrome.devtools.json");
        if (configuredUrls != null) {
            configuredUrls.stream()
                    .filter(url -> url != null && !url.isBlank())
                    .map(this::normalizeUrl)
                    .forEach(urls::add);
        }
        return Set.copyOf(urls);
    }

    private List<String> loadSilentNotFoundUrls(Environment environment) {
        if (environment == null) {
            return List.of();
        }
        return Binder.get(environment)
                .bind("isass.web.exception.silent-not-found-urls", Bindable.listOf(String.class))
                .orElse(List.of());
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String normalized = url.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    /**
     * 当没有 IExceptionMapping 时，从异常本身记录信息进行消息格式化
     *
     * @param t 被抛出的异常
     * @return 格式化后的消息
     */
    private String defaultMessage(Throwable t) {
        return t.getClass().getSimpleName() + ((t.getMessage() == null) ? "" : (": " + t.getMessage()));
    }

    public Resp<?> createRespByException(Exception e) {
        Resp<?> resp = null;
        boolean mapped = e instanceof UnifiedException;
        if (e instanceof UnifiedException) {
            UnifiedException exception = (UnifiedException) e;

            Exception cause = (Exception) exception.getCause();
            if (exception.getStatus() == null && cause != null) {
                resp = createRespByExceptionFromExceptionMappings(cause);
            }

            resp = resp == null
                    ? new Resp<>()
                    .setSuccess(false)
                    .setStatus(ObjectUtil.defaultIfNull(exception.getStatus(), StatusMessageEnum.UNDEFINED.getStatus()))
                    .setMessage(ObjectUtil.defaultIfNull(exception.getMsg(), defaultMessage(exception)))
                    : resp;
        } else {
            resp = createRespByExceptionFromExceptionMappings(e);
            mapped = resp != null;
        }
        if (resp == null) {
            resp = new Resp<>()
                    .setSuccess(Boolean.FALSE)
                    .setStatus(StatusMessageEnum.UNDEFINED.getStatus())
                    .setMessage(defaultMessage(ExceptionUtil.unwrap(e)));
        }
        String traceId = LongSequence.get().toString();
        if (showDetailError) {
            resp.setDetailMessage("[" + traceId + "]\n" + conciseStackTrace(ExceptionUtil.unwrap(e)));
        } else {
            if (!mapped) {
                resp.setMessage("[" + traceId + "] " + prodUnifiedMessage);
            }
            resp.setDetailMessage(null);
        }
        return resp;
    }

    private String conciseStackTrace(Throwable throwable) {
        List<String> lines = new ArrayList<>();
        Throwable current = throwable;
        for (int causeIndex = 0; current != null && causeIndex < 3 && lines.size() < 30; causeIndex++) {
            lines.add((causeIndex == 0 ? "" : "Caused by: ")
                    + current.getClass().getSimpleName()
                    + (current.getMessage() == null ? "" : ": " + current.getMessage()));
            List<StackTraceElement> businessFrames = java.util.Arrays.stream(current.getStackTrace())
                    .filter(frame -> frame.getClassName().startsWith("vip.isass."))
                    .limit(8)
                    .toList();
            List<StackTraceElement> frames = businessFrames.isEmpty()
                    ? java.util.Arrays.stream(current.getStackTrace()).limit(5).toList()
                    : businessFrames;
            for (StackTraceElement frame : frames) {
                if (lines.size() >= 29) {
                    lines.add("... truncated");
                    break;
                }
                lines.add("\tat " + frame);
            }
            current = current.getCause();
        }
        if (current != null && lines.size() < 30) {
            lines.add("... truncated");
        }
        String result = String.join("\n", lines);
        if (result.length() > 8192) {
            return result.substring(0, 8178) + "\n... truncated";
        }
        return result;
    }

    private Resp<?> createRespByExceptionFromExceptionMappings(Exception e) {
        for (IExceptionMapping exceptionMapping : exceptionMappings) {
            IStatusMessage statusMessage = exceptionMapping.getStatusCode(e);
            if (statusMessage == null) {
                continue;
            }

            return new Resp<>()
                    .setSuccess(false)
                    .setStatus(statusMessage.getStatus())
                    .setMessage(exceptionMapping.parseMessage(e, statusMessage));
        }
        return null;
    }

    /**
     * 处理错误消息的返回策略 - 根据开关配置控制错误信息返回
     * 包含traceId用于日志排查，生产环境可屏蔽详细堆栈信息
     *
     * @param message 原始消息
     * @return 处理后的消息（包含traceId和控制后的错误信息）
     */
    public String processErrorMessage(String message) {
        return processErrorMessageResult(message).message();
    }

    private ProcessedErrorMessage processErrorMessageResult(String message) {
        String traceId = LongSequence.get().toString();
        String detailMessage = "[" + traceId + "] " + message;

        if (showDetailError) {
            // 显示详细错误信息，包含traceId
            log.info("[ERROR_TRACE] 返回详细错误信息 - traceId: {}, message: {}", traceId, message);
            return new ProcessedErrorMessage(detailMessage, detailMessage);
        } else {
            // 不显示详情，返回统一错误提示，但仍包含traceId
            String resultMessage = "[" + traceId + "] " + prodUnifiedMessage;
            log.info("[ERROR_TRACE] 返回统一错误信息 - traceId: {}, originalMessage: {}, unifiedMessage: {}",
                    traceId, message, prodUnifiedMessage);
            return new ProcessedErrorMessage(resultMessage, detailMessage);
        }
    }

    private record ProcessedErrorMessage(String message, String detailMessage) {
    }

}
