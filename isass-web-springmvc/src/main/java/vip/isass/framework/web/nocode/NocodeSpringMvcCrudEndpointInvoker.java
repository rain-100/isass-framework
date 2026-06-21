package vip.isass.framework.web.nocode;

import vip.isass.framework.nocode.v3.access.NocodeAccessHandler;
import vip.isass.framework.nocode.v3.access.NocodeAccessRequest;

import java.util.Objects;

/**
 * Invokes nocode v3 CRUD operations for Spring MVC dynamic endpoints.
 */
public class NocodeSpringMvcCrudEndpointInvoker {

    private final NocodeAccessHandler accessHandler;
    private final NocodeSpringMvcCrudRequestFactory requestFactory;

    public NocodeSpringMvcCrudEndpointInvoker(NocodeAccessHandler accessHandler) {
        this(accessHandler, new NocodeSpringMvcCrudRequestFactory());
    }

    public NocodeSpringMvcCrudEndpointInvoker(
            NocodeAccessHandler accessHandler,
            NocodeSpringMvcCrudRequestFactory requestFactory
    ) {
        this.accessHandler = Objects.requireNonNull(accessHandler, "accessHandler");
        this.requestFactory = Objects.requireNonNull(requestFactory, "requestFactory");
    }

    public <R> R invoke(
            NocodeSpringMvcCrudRoute route,
            NocodeSpringMvcCrudRequestArguments arguments
    ) {
        NocodeAccessRequest request = requestFactory.create(route, arguments);
        return accessHandler.handle(request);
    }
}
