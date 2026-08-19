package com.javatodev.finance.fixture;

import com.javatodev.finance.model.TransactionType;
import com.javatodev.finance.model.entity.BankAccountEntity;
import com.javatodev.finance.model.entity.TransactionEntity;

import java.math.BigDecimal;

public final class TransactionEntityBuilder {

    private final TransactionEntity transaction = TransactionEntity.builder()
        .transactionType(TransactionType.FUND_TRANSFER)
        .amount(BigDecimal.ZERO)
        .build();

    private TransactionEntityBuilder() {
    }

    public static TransactionEntityBuilder aTransaction() {
        return new TransactionEntityBuilder();
    }

    public TransactionEntityBuilder withAccount(BankAccountEntity account) {
        transaction.setAccount(account);
        return this;
    }

    public TransactionEntityBuilder withAmount(String amount) {
        transaction.setAmount(new BigDecimal(amount));
        return this;
    }

    public TransactionEntityBuilder withTransactionId(String transactionId) {
        transaction.setTransactionId(transactionId);
        return this;
    }

    public TransactionEntityBuilder withReferenceNumber(String referenceNumber) {
        transaction.setReferenceNumber(referenceNumber);
        return this;
    }

    public TransactionEntityBuilder withType(TransactionType type) {
        transaction.setTransactionType(type);
        return this;
    }

    public TransactionEntity build() {
        return transaction;
    }
}
