// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.adapter.springboot.condition;

public enum IsassFeature {

    DATABASE_CORE("vip.isass.framework.database.core.exception.DatabaseExceptionMapping"),

    MQ_CORE("vip.isass.framework.mq.core.MqManager");

    private final String markerClassName;

    IsassFeature(String markerClassName) {
        this.markerClassName = markerClassName;
    }

    public String getMarkerClassName() {
        return markerClassName;
    }
}
