package vip.isass.framework.nocode.security;

/** Authorizes a resolved nocode invocation before the local service is called. */
@FunctionalInterface
public interface NocodePermissionEvaluator {

    NocodePermissionEvaluator ALLOW_ALL = context -> { };

    void check(NocodeAuthorizationContext context);
}
