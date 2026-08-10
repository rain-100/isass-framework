// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import java.util.Date;

/**
 * @author Rain
 */
public interface ISystemClock {


    long now();

    Date nowDate();

}
