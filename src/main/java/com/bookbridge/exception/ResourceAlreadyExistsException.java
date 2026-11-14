package com.bookbridge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when creating a resource that already exists.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceAlreadyExistsException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a duplicate-resource error, for example when registering with an email
     * that already has an account.
     *
     * @param message description of the conflicting resource
     */
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
