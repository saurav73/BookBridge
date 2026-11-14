package com.bookbridge.exception;

/**
 * Thrown when an uploaded file cannot be stored on disk.
 */
public class FileStorageException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a file-storage failure with a description of what went wrong.
     *
     * @param message description of the storage failure
     */
    public FileStorageException(String message) {
        super(message);
    }
    
    /**
     * Creates a file-storage failure wrapping the underlying I/O (or other) cause.
     *
     * @param message description of the storage failure
     * @param cause underlying exception that caused the failure
     */
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
