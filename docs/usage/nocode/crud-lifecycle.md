# CRUD Lifecycle Callbacks

`ILocalService` provides lifecycle callbacks around every standard write operation without relying on Spring AOP or any other runtime framework. The feature is implemented by the pure Java `CrudLifecycleRegistry`, so another container only needs to create and register listeners.

## Callback Order

For a top-level standard CRUD call, callbacks run in this order:

1. `before`: before the repository operation. Throw an exception here to prevent the write.
2. `afterSuccess`: after the repository operation returns successfully. Use it for cache invalidation and lightweight business follow-up.
3. `onFailure`: when the operation or an earlier callback throws. The original exception is preserved.

Nested standard CRUD calls do not produce another callback sequence. For example, `batchSave` internally adds, updates and deletes records, but listeners receive one `BATCH_SAVE` context only.

The registry is framework-independent and does not observe transaction commit. If an outer transaction rolls back after `afterSuccess`, a cache invalidation may cause one additional database read, but it cannot expose stale authorization data.

## Register A Listener

Create a listener and register it when the application starts. In a Spring application, the container may construct the listener, but the lifecycle mechanism itself does not depend on Spring.

```java
public final class UserCacheListener implements CrudLifecycleListener {

    @Override
    public boolean supports(CrudLifecycleContext context) {
        return context.entityClass() == User.class;
    }

    @Override
    public void before(CrudLifecycleContext context) {
        // For deletes or criteria updates, query and store affected IDs here.
        context.attributes().put("userIds", Set.of(1001L));
    }

    @Override
    public void afterSuccess(CrudLifecycleContext context) {
        Set<Long> userIds = (Set<Long>) context.attributes().get("userIds");
        userIds.forEach(userCache::evict);
    }

    @Override
    public void onFailure(CrudLifecycleContext context, Throwable error) {
        // Release temporary state or record an audit event.
    }
}

CrudLifecycleRegistry.register(new UserCacheListener());
```

Call `CrudLifecycleRegistry.unregister(listener)` when a runtime unloads the listener.

## Context

`CrudLifecycleContext` contains:

- `service()`: the current `ILocalService` implementation.
- `entityClass()`: the model class handled by the service.
- `operation()`: `ADD`, `ADD_BATCH`, `ADD_IF_ABSENT`, `ADD_OR_UPDATE`, `UPDATE`, `DELETE`, or `BATCH_SAVE`.
- `methodName()` and `arguments()`: the original standard CRUD method invocation.
- `result()`: available after the operation succeeds.
- `attributes()`: a per-invocation map shared across callbacks.

Use `attributes()` to capture old relationship data in `before`, especially for `deleteById`, `deleteByCriteria` and `updateByCriteria`.

## Authorization Cache Example

BSP uses this mechanism for authorization relationship caches:

- `UserRole` clears `user-role-ids:{userId}`.
- `OrgUser` clears `user-org-ids:{userId}`.
- `OrgRole` clears `org-role-ids:{orgId}`.
- `UserPosition` clears `user-position-ids:{userId}`.
- `PositionRole` clears `position-role-ids:{positionId}`.
- `Role` clears `role-code:{roleId}`.

The listener snapshots affected relationship records in `before`, then removes only their keys in `afterSuccess`. This avoids global invalidation when a tenant has a large number of users.
