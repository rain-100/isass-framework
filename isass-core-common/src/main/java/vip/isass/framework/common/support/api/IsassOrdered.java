package vip.isass.framework.common.support.api;

/**
 * isass ordering contract that does not depend on any runtime framework.
 *
 * @author isass
 */
public interface IsassOrdered {

    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    default int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
