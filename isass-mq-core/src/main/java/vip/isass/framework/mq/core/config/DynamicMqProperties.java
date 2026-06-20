package vip.isass.framework.mq.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "isass.mq")
public class DynamicMqProperties {

    private Boolean enabled = Boolean.FALSE;

    private String primary = "master";

    private Map<String, MqSourceProperties> sources = new HashMap<>();
}
