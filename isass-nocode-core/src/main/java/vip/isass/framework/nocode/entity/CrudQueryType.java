// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

/** Canonical query modes executed by {@code CrudQueryExecutor}. */
public enum CrudQueryType {
    PAGE,
    CURSOR_PAGE,
    COUNT,
    EXISTS
}
