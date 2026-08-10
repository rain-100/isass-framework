// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.exception;

/**
 * @author Rain
 */
public class AbsentException extends RuntimeException {

    public AbsentException() {
        super();
    }

    public AbsentException(String message) {
        super(message);
    }

    public AbsentException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbsentException(Throwable cause) {
        super(cause);
    }

}
