package com.bookbridge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when registering an account that already exists.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a duplicate-user error, raised when registration uses an email that
     * already has an account.
     *
     * @param message description of the duplicate user
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
