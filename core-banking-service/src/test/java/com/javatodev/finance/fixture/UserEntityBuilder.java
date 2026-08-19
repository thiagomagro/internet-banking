package com.javatodev.finance.fixture;

import com.javatodev.finance.model.entity.UserEntity;

public final class UserEntityBuilder {

    private final UserEntity user = new UserEntity();

    private UserEntityBuilder() {
    }

    public static UserEntityBuilder aUser() {
        return new UserEntityBuilder();
    }

    public UserEntityBuilder withIdentificationNumber(String identificationNumber) {
        user.setIdentificationNumber(identificationNumber);
        return this;
    }

    public UserEntityBuilder withEmail(String email) {
        user.setEmail(email);
        return this;
    }

    public UserEntityBuilder withName(String firstName, String lastName) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return this;
    }

    public UserEntity build() {
        return user;
    }
}
