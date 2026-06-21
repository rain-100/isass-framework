package vip.isass.framework.nocode.v2.entity;

/**
 * Legacy v2 database entity marker.
 *
 * @author Rain
 */
public interface IV2DbEntity<E extends IV2Entity<E>, EDB extends IV2DbEntity<E, EDB>>
        extends IV2Entity<E> {

    @SuppressWarnings("unchecked")
    default E convertToEntity() {
        return V2DbEntityConvert.convertToEntity((EDB) this);
    }

}
