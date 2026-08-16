// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria;

public interface IUpdateCriteria<C extends IUpdateCriteria<C>> {

    UpdateMode getUpdateMode();

    C setUpdateMode(UpdateMode updateMode);

    NullValueMode getNullValueMode();

    C setNullValueMode(NullValueMode nullValueMode);

    default UpdateMode resolveUpdateMode() {
        return getUpdateMode() == null ? UpdateMode.MERGE : getUpdateMode();
    }

    default NullValueMode resolveNullValueMode() {
        return getNullValueMode() == null ? NullValueMode.IGNORE_NULL : getNullValueMode();
    }
}
