// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.exception;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import vip.isass.framework.common.exception.IStatusMapping;
import vip.isass.framework.common.exception.UnifiedException;
import vip.isass.framework.common.exception.code.IStatusMessage;
import vip.isass.framework.common.exception.code.StatusMessageEnum;
import vip.isass.framework.common.support.IsassServiceLoader;
import vip.isass.framework.common.web.Resp;

// import javax.annotation.Resource;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author Rain
 */
@RestController
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class IsassErrorController implements ErrorController {

    private static final String PATH = "/error";

    private final ErrorAttributes errorAttributes;

    private final List<IStatusMapping> statusMappings;

    public IsassErrorController(ErrorAttributes errorAttributes) {
        this(errorAttributes, List.of());
    }

    @Autowired
    public IsassErrorController(ErrorAttributes errorAttributes, @Autowired(required = false) List<IStatusMapping> statusMappings) {
        Assert.notNull(errorAttributes, "ErrorAttributes must not be null");
        this.errorAttributes = errorAttributes;
        this.statusMappings = IsassServiceLoader.mergeByClass(
                statusMappings == null ? List.of() : statusMappings,
                IsassServiceLoader.load(IStatusMapping.class)
        );
    }

    // @Override
    public String getErrorPath() {
        return PATH;
    }

    /**
     * 处理还没进入 controller 就抛出的异常
     *
     * @param request  request
     * @param response response
     * @return resp
     */
    @RequestMapping(value = PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public Resp<?> errorJson(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> errorAttributes = getErrorAttributes(request, true);
        Integer status = resolveStatus(errorAttributes, response);
        response.setStatus(status);
        if (!shouldReturnJsonResponse(request)) {
            return null;
        }

        Object exception = errorAttributes.get(RequestDispatcher.ERROR_EXCEPTION);
        if (exception instanceof UnifiedException) {
            return new Resp<>()
                    .setSuccess(Boolean.FALSE)
                    .setStatus(((UnifiedException) exception).getStatus())
                    .setMessage(((UnifiedException) exception).getMsg());
        }

        for (IStatusMapping statusMapping : statusMappings) {
            IStatusMessage statusCode = statusMapping.getErrorCode(status);
            if (statusCode == null) {
                continue;
            }
            return new Resp<>()
                    .setSuccess(false)
                    .setStatus(statusCode.getStatus())
                    .setMessage(formatErrorMessage(statusCode.getMsg(), errorAttributes));
        }

        return new Resp<>()
                .setSuccess(false)
                .setStatus(status)
                .setMessage(formatErrorMessage(StatusMessageEnum.UNDEFINED.getMsg(), errorAttributes));
    }

    private String formatErrorMessage(String statusMessage, Map<String, Object> errorAttributes) {
        return java.util.stream.Stream.of(
                        statusMessage + ":",
                        errorAttributes.get("path"),
                        errorAttributes.get("error"),
                        errorAttributes.get("exception"),
                        errorAttributes.get("message"))
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private Integer resolveStatus(Map<String, Object> errorAttributes, HttpServletResponse response) {
        Object status = errorAttributes.get("status");
        if (status instanceof Number number) {
            return number.intValue();
        }
        if (status != null && StrUtil.isNotBlank(status.toString())) {
            try {
                return Integer.valueOf(status.toString());
            } catch (NumberFormatException ignored) {
                // Fall back to servlet response status below.
            }
        }
        return response.getStatus() > 0 ? response.getStatus() : 500;
    }

    private boolean shouldReturnJsonResponse(HttpServletRequest request) {
        if (containsMediaType(request.getContentType(), MediaType.APPLICATION_JSON)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        if (containsMediaType(accept, MediaType.APPLICATION_JSON)) {
            return true;
        }
        return !containsMediaType(accept, MediaType.TEXT_HTML);
    }

    private boolean containsMediaType(String headerValue, MediaType expected) {
        if (StrUtil.isBlank(headerValue)) {
            return false;
        }
        try {
            return MediaType.parseMediaTypes(headerValue)
                    .stream()
                    .anyMatch(mediaType -> expected.isCompatibleWith(mediaType));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    protected Map<String, Object> getErrorAttributes(HttpServletRequest request, boolean includeStackTrace) {
        ServletWebRequest servletWebRequest = new ServletWebRequest(request);
        Map<String, Object> errorAttributes = this.errorAttributes.getErrorAttributes(servletWebRequest, org.springframework.boot.web.error.ErrorAttributeOptions.defaults());
        Throwable error = this.errorAttributes.getError(servletWebRequest);
        if (error != null) {
            errorAttributes.put(RequestDispatcher.ERROR_EXCEPTION, error);
        }
        return errorAttributes;
    }

}
