package vip.isass.framework.nocode.v3.query;

public record NocodePageRequest(int pageNumber, int pageSize) {

    public NocodePageRequest {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be greater than 0");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
    }
}
