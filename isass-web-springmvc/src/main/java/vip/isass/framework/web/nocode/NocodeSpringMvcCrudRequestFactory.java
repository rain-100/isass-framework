package vip.isass.framework.web.nocode;

import vip.isass.framework.nocode.v3.access.NocodeAccessRequest;
import vip.isass.framework.nocode.v3.access.NocodeCrudAccessRequests;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;

import java.util.Objects;

/**
 * Creates framework-neutral nocode access requests from Spring MVC CRUD route inputs.
 */
public class NocodeSpringMvcCrudRequestFactory {

    public NocodeAccessRequest create(
            NocodeSpringMvcCrudRoute route,
            NocodeSpringMvcCrudRequestArguments arguments
    ) {
        Objects.requireNonNull(route, "route");
        return create(route.operation(), arguments);
    }

    public NocodeAccessRequest create(
            NocodeCrudOperation operation,
            NocodeSpringMvcCrudRequestArguments arguments
    ) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(arguments, "arguments");
        return switch (operation) {
            case FIND_BY_ID -> findById(arguments);
            case PAGE -> page(arguments);
            case LIST -> list(arguments);
            case SAVE -> NocodeCrudAccessRequests.save(
                    arguments.entityName(),
                    arguments.body(),
                    arguments.returnType()
            );
            case UPDATE_BY_ID -> NocodeCrudAccessRequests.updateById(
                    arguments.entityName(),
                    arguments.id(),
                    arguments.body(),
                    arguments.returnType()
            );
            case DELETE_BY_ID -> deleteById(arguments);
        };
    }

    private NocodeAccessRequest findById(NocodeSpringMvcCrudRequestArguments arguments) {
        if (arguments.fetchOptions() == null) {
            return NocodeCrudAccessRequests.findById(arguments.entityName(), arguments.id(), arguments.returnType());
        }
        return NocodeCrudAccessRequests.findById(
                arguments.entityName(),
                arguments.id(),
                arguments.fetchOptions(),
                arguments.returnType()
        );
    }

    private NocodeAccessRequest page(NocodeSpringMvcCrudRequestArguments arguments) {
        if (arguments.fetchOptions() == null) {
            return NocodeCrudAccessRequests.page(arguments.entityName(), arguments.criteria(), arguments.returnType());
        }
        return NocodeCrudAccessRequests.page(
                arguments.entityName(),
                arguments.criteria(),
                arguments.fetchOptions(),
                arguments.returnType()
        );
    }

    private NocodeAccessRequest list(NocodeSpringMvcCrudRequestArguments arguments) {
        if (arguments.fetchOptions() == null) {
            return NocodeCrudAccessRequests.list(arguments.entityName(), arguments.criteria(), arguments.returnType());
        }
        return NocodeCrudAccessRequests.list(
                arguments.entityName(),
                arguments.criteria(),
                arguments.fetchOptions(),
                arguments.returnType()
        );
    }

    private NocodeAccessRequest deleteById(NocodeSpringMvcCrudRequestArguments arguments) {
        if (arguments.deleteOptions() == null) {
            return NocodeCrudAccessRequests.deleteById(arguments.entityName(), arguments.id(), arguments.returnType());
        }
        return NocodeCrudAccessRequests.deleteById(
                arguments.entityName(),
                arguments.id(),
                arguments.deleteOptions(),
                arguments.returnType()
        );
    }
}
