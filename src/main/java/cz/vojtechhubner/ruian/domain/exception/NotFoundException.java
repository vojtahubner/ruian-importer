package cz.vojtechhubner.ruian.domain.exception;

/**
 * A queried resource (a municipality, an import run) does not exist.
 */
public class NotFoundException extends RuntimeException {

    private final String resource;
    private final Object id;

    public NotFoundException(String resource, Object id) {
        super(resource + " " + id + " not found");
        this.resource = resource;
        this.id = id;
    }

    public String resource() {
        return resource;
    }

    public Object id() {
        return id;
    }
}
