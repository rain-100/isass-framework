// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.entrypoint.IEntrypoint;

/**
 * 面向跨聚合用例的应用服务合同。
 *
 * <p>应用服务只声明显式的业务操作，不继承 {@link ICrudService} 的标准 CRUD 操作。</p>
 */
public interface IApplicationService extends IEntrypoint {
}
