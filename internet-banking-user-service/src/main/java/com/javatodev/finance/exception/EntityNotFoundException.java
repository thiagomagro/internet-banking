package com.javatodev.finance.exception;

public class EntityNotFoundException extends SimpleBankingGlobalException {
    public EntityNotFoundException() {
        super(GlobalErrorCode.ERROR_ENTITY_NOT_FOUND, "Requested entity not present in the DB.");
    }

    public EntityNotFoundException (String message) {
        super(GlobalErrorCode.ERROR_ENTITY_NOT_FOUND, message);
    }
}
