package com.alpian.paymentws.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when a customer is not found.
 * Returns HTTP 404 Not Found.
 */
public class CustomerNotFoundException extends ResponseStatusException {

    private static final long serialVersionUID = -5575576332962208547L;

	public CustomerNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
