package vip.isass.framework.web.servicedocs;

/**
 * @author Rain
 */
public record ServiceDoc(
        String id,
        String title,
        String type,
        String format,
        String path
) {
}
