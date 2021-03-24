package org.jfrog.util.docker.exceptions

public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException(String message) {
        super(message)
    }
}
