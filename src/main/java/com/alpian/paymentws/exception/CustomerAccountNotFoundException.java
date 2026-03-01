package com.alpian.paymentws.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when a customer account is not found.
 * Returns HTTP 404 Not Found.
 */
public class CustomerAccountNotFoundException extends ResponseStatusException {

    private static final long serialVersionUID = 7368911974773732940L;

	public CustomerAccountNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
