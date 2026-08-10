// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.lifecycle;

/** Standard nocode write operations that can publish lifecycle callbacks. */
public enum CrudOperation {
    ADD,
    ADD_BATCH,
    ADD_IF_ABSENT,
    ADD_OR_UPDATE,
    UPDATE,
    DELETE,
    BATCH_SAVE;

    public static CrudOperation fromMethodName(String methodName) {
        if (methodName == null) return null;
        if (methodName.equals("batchSave")) return BATCH_SAVE;
        if (methodName.startsWith("addOrUpdate")) return ADD_OR_UPDATE;
        if (methodName.startsWith("addIfAbsent")) return ADD_IF_ABSENT;
        if (methodName.startsWith("addBatch")) return ADD_BATCH;
        if (methodName.equals("add")) return ADD;
        if (methodName.startsWith("update")) return UPDATE;
        if (methodName.startsWith("delete")) return DELETE;
        return null;
    }
}
