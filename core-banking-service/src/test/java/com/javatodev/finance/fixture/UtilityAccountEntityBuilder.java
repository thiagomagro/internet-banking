package com.javatodev.finance.fixture;

import com.javatodev.finance.model.entity.UtilityAccountEntity;

public final class UtilityAccountEntityBuilder {

    private final UtilityAccountEntity account = new UtilityAccountEntity();

    private UtilityAccountEntityBuilder() {
    }

    public static UtilityAccountEntityBuilder aUtilityAccount() {
        return new UtilityAccountEntityBuilder();
    }

    public UtilityAccountEntityBuilder withNumber(String number) {
        account.setNumber(number);
        return this;
    }

    public UtilityAccountEntityBuilder withProviderName(String providerName) {
        account.setProviderName(providerName);
        return this;
    }

    public UtilityAccountEntity build() {
        return account;
    }
}
