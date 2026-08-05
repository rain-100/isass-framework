package vip.isass.framework.nocode.property;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyNameResolverTest {

    @Test
    void resolvesStandardGetterMethodReferenceToCamelCasePropertyName() {
        assertEquals("nickName", PropertyNameResolver.resolve(UserView::getNickName));
    }

    @Test
    void resolvesBooleanGetterMethodReferenceToCamelCasePropertyName() {
        assertEquals("enabled", PropertyNameResolver.resolve(UserView::isEnabled));
    }

    private static final class UserView {
        private String nickName;
        private boolean enabled;

        String getNickName() {
            return nickName;
        }

        boolean isEnabled() {
            return enabled;
        }
    }
}
