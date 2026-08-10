// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.exception;

import vip.isass.framework.common.exception.code.IStatusMessage;

/**
 * @author Rain
 */
public interface IStatusMapping {

    IStatusMessage getErrorCode(Integer code);

}