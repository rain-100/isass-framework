package vip.isass.framework.nocode.v3.model;

/**
 * Auto-fill timing for nocode v3 fields.
 */
public enum NocodeFieldAutoFill {

    NONE(false, false),
    CREATE_TIME(true, false),
    UPDATE_TIME(false, true),
    CREATE_AND_UPDATE_TIME(true, true);

    private final boolean fillOnCreate;
    private final boolean fillOnUpdate;

    NocodeFieldAutoFill(boolean fillOnCreate, boolean fillOnUpdate) {
        this.fillOnCreate = fillOnCreate;
        this.fillOnUpdate = fillOnUpdate;
    }

    public boolean fillOnCreate() {
        return fillOnCreate;
    }

    public boolean fillOnUpdate() {
        return fillOnUpdate;
    }
}
