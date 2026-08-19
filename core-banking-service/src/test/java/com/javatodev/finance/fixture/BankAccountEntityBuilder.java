package com.javatodev.finance.fixture;

import com.javatodev.finance.model.AccountStatus;
import com.javatodev.finance.model.AccountType;
import com.javatodev.finance.model.entity.BankAccountEntity;

import java.math.BigDecimal;

public final class BankAccountEntityBuilder {

    private final BankAccountEntity account = new BankAccountEntity();

    private BankAccountEntityBuilder() {
        account.setType(AccountType.SAVINGS_ACCOUNT);
        account.setStatus(AccountStatus.ACTIVE);
        account.setActualBalance(BigDecimal.ZERO);
        account.setAvailableBalance(BigDecimal.ZERO);
    }

    public static BankAccountEntityBuilder anAccount() {
        return new BankAccountEntityBuilder();
    }

    public BankAccountEntityBuilder withNumber(String number) {
        account.setNumber(number);
        return this;
    }

    public BankAccountEntityBuilder withBalance(String balance) {
        BigDecimal value = new BigDecimal(balance);
        account.setActualBalance(value);
        account.setAvailableBalance(value);
        return this;
    }

    public BankAccountEntityBuilder withActualBalance(String balance) {
        account.setActualBalance(new BigDecimal(balance));
        return this;
    }

    public BankAccountEntityBuilder withAvailableBalance(String balance) {
        account.setAvailableBalance(new BigDecimal(balance));
        return this;
    }

    public BankAccountEntity build() {
        return account;
    }
}
