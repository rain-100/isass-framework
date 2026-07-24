package vip.isass.framework.nocode.lifecycle;

import vip.isass.framework.nocode.service.ILocalService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/** Pure Java registry that surrounds standard nocode CRUD operations with callbacks. */
public final class CrudLifecycleRegistry {

    private static final List<CrudLifecycleListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private CrudLifecycleRegistry() {
    }

    public static void register(CrudLifecycleListener listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    public static void unregister(CrudLifecycleListener listener) {
        LISTENERS.remove(listener);
    }

    public static <T> T execute(ILocalService<?, ?> service, CrudOperation operation,
                                String methodName, Object[] arguments, Supplier<T> action) {
        if (DEPTH.get() > 0) return action.get();
        DEPTH.set(1);
        CrudLifecycleContext context = new CrudLifecycleContext(service, operation, methodName, arguments);
        List<CrudLifecycleListener> listeners = LISTENERS.stream().filter(listener -> listener.supports(context)).toList();
        try {
            listeners.forEach(listener -> listener.before(context));
            T result = action.get();
            context.setResult(result);
            listeners.forEach(listener -> listener.afterSuccess(context));
            return result;
        } catch (RuntimeException | Error error) {
            for (CrudLifecycleListener listener : listeners) {
                try {
                    listener.onFailure(context, error);
                } catch (RuntimeException | Error callbackError) {
                    error.addSuppressed(callbackError);
                }
            }
            throw error;
        } finally {
            DEPTH.remove();
        }
    }
}
