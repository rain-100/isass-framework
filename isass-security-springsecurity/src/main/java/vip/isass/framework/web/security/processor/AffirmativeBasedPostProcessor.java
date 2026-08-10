// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;


/**
 * @author Rain
 */
public class AffirmativeBasedPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // AffirmativeBased is no longer used in Spring Security 7 native model.
        // This processor is now a placeholder.
        return bean;
    }
}
