package com.nttdata.transaction_service.domain.error;

public class AccountValidationException extends RuntimeException {
    public AccountValidationException(String message) {
        super(message);
    }
}
