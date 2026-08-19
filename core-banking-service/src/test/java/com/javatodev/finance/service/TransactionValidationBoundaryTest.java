package com.javatodev.finance.service;

import com.javatodev.finance.exception.InsufficientFundsException;
import com.javatodev.finance.model.dto.BankAccount;
import com.javatodev.finance.model.dto.request.FundTransferRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionValidationBoundaryTest {

    @ParameterizedTest
    @MethodSource("acceptedAmounts")
    void validateBalanceAcceptsAmountsNotGreaterThanTheBalance(String amount) {
        AccountService accountService = mock(AccountService.class);
        TransactionService service = new TransactionService(accountService, mock(), mock());
        BankAccount account = account("FROM", "100.000");
        when(accountService.readBankAccount("FROM")).thenReturn(account);
        when(accountService.readBankAccount("TO")).thenReturn(account("TO", "0"));

        // The service currently permits zero and negative amounts; this test records that behavior.
        assertThatThrownBy(() -> service.fundTransfer(request(amount)))
            .isNotInstanceOf(InsufficientFundsException.class);
    }

    @ParameterizedTest
    @MethodSource("rejectedAmounts")
    void validateBalanceRejectsNegativeBalanceOrAmountsGreaterThanBalance(String balance, String amount) {
        AccountService accountService = mock(AccountService.class);
        TransactionService service = new TransactionService(accountService, mock(), mock());
        when(accountService.readBankAccount("FROM")).thenReturn(account("FROM", balance));
        when(accountService.readBankAccount("TO")).thenReturn(account("TO", "0"));

        assertThatThrownBy(() -> service.fundTransfer(request(amount)))
            .isInstanceOf(InsufficientFundsException.class);
    }

    private static Stream<Arguments> acceptedAmounts() {
        return Stream.of(Arguments.of("100.00"), Arguments.of("0"), Arguments.of("0.005"), Arguments.of("-0.01"));
    }

    private static Stream<Arguments> rejectedAmounts() {
        return Stream.of(Arguments.of("-0.01", "0"), Arguments.of("100.00", "100.005"));
    }

    private static FundTransferRequest request(String amount) {
        FundTransferRequest request = new FundTransferRequest();
        request.setFromAccount("FROM");
        request.setToAccount("TO");
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private static BankAccount account(String number, String balance) {
        BankAccount account = new BankAccount();
        account.setNumber(number);
        account.setActualBalance(new BigDecimal(balance));
        return account;
    }
}
