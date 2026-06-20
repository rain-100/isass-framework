package vip.isass.framework.mq.core.config;

import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class MqSourceProperties {

    private Boolean enabled = Boolean.FALSE;

    private String name;

    private String type;

    private Map<String, Object> options = new HashMap<>();
}
