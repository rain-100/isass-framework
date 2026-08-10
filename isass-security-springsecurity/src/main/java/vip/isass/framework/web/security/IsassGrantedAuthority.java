// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security;

import cn.hutool.core.lang.Assert;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Objects;

public class IsassGrantedAuthority implements GrantedAuthority {

    @Getter
    private final String roleCode;

    public IsassGrantedAuthority(String roleCode) {
        Assert.notBlank(roleCode, "roleCode");
        this.roleCode = roleCode;
    }

    @Override
    public String getAuthority() {
        return roleCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsassGrantedAuthority that = (IsassGrantedAuthority) o;
        return Objects.equals(roleCode, that.roleCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleCode);
    }

    @Override
    public String toString() {
        return roleCode;
    }

}