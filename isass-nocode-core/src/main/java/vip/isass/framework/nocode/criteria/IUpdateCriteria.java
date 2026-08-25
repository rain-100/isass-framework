// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria;

import java.util.Collection;
import java.util.List;

public interface IUpdateCriteria<C extends IUpdateCriteria<C>> {

    UpdateMode getUpdateMode();

    C setUpdateMode(UpdateMode updateMode);

    NullValueMode getNullValueMode();

    C setNullValueMode(NullValueMode nullValueMode);

    Collection<String> getMatchFields();

    C setMatchFields(Collection<String> matchFields);

    default List<String> resolveMatchFields() {
        if (getMatchFields() == null) {
            return List.of();
        }
        return getMatchFields().stream()
                .filter(field -> field != null && !field.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    default UpdateMode resolveUpdateMode() {
        return getUpdateMode() == null ? UpdateMode.MERGE : getUpdateMode();
    }

    default NullValueMode resolveNullValueMode() {
        return getNullValueMode() == null ? NullValueMode.IGNORE_NULL : getNullValueMode();
    }
}
