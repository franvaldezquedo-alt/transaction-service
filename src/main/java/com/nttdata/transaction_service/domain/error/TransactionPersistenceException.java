package com.nttdata.transaction_service.domain.error;

public class TransactionPersistenceException extends RuntimeException {
    public TransactionPersistenceException(String message) {
        super(message);
    }
}
