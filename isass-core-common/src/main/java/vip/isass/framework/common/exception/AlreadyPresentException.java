// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.exception;

/**
 * @author Rain
 */
public class AlreadyPresentException extends RuntimeException {

    public AlreadyPresentException() {
        super();
    }

    public AlreadyPresentException(String message) {
        super(message);
    }

    public AlreadyPresentException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyPresentException(Throwable cause) {
        super(cause);
    }

}
