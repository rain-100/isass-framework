package vip.isass.core.web.security.config;

import lombok.Getter;

/**
 * 接口访问安全策略
 */
public enum UrlAccessSecurityStrategy {

    NONE("不开启权限管理，所有请求都能访问（除了某些业务接口，代码里用到登陆用户的信息）"),
    AUTHENTICATED("登录认证成功后，才能访问接口（除了代码指定不用token的接口（例如登录接口），其他接口都需要token才能访问）"),
    ROLE("基于角色认证的权限管理，需要在权限前端配置访问每个接口所需要的权限角色，每个用户需要赋予对应的角色");

    @Getter
    private final String desc;

    UrlAccessSecurityStrategy(String desc) {
        this.desc = desc;
    }

}
