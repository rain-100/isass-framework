package vip.isass.framework.nocode.v3.entity;

/**
 * Legacy v2 database entity marker.
 *
 * @author Rain
 */
public interface IV3DbEntity<E extends IV3Entity<E>, EDB extends IV3DbEntity<E, EDB>>
        extends IV3Entity<E> {

    @SuppressWarnings("unchecked")
    default E convertToEntity() {
        return V3DbEntityConvert.convertToEntity((EDB) this);
    }

}
