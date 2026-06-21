package vip.isass.framework.adapter.springboot.destroy;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import vip.isass.framework.common.spring.bean.destroy.AutoDestroyable;
import vip.isass.framework.common.support.BeanProviderUtil;

/**
 * Automatically unregisters beans that are only needed during startup.
 *
 * @author rain
 */
public class AutoDestroyManager {

    @EventListener(ApplicationReadyEvent.class)
    public void destroy(ApplicationReadyEvent event) {
        BeanProviderUtil.unRegistryBean(AutoDestroyable.class);
    }
}
