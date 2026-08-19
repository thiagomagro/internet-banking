package com.javatodev.finance.service;

import com.javatodev.finance.model.dto.BankAccount;
import com.javatodev.finance.model.entity.BankAccountEntity;
import com.javatodev.finance.repository.BankAccountRepository;
import com.javatodev.finance.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvailableBalanceInvariantTest {

    @Test
    void internalTransferChangesAvailableBalanceByTheSameDeltaAsActualBalance() {
        AccountService accountService = mock(AccountService.class);
        BankAccountRepository bankAccountRepository = mock(BankAccountRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        TransactionService service = new TransactionService(accountService, bankAccountRepository, transactionRepository);

        BankAccount from = new BankAccount();
        from.setNumber("FROM");
        BankAccount to = new BankAccount();
        to.setNumber("TO");

        BankAccountEntity fromEntity = new BankAccountEntity();
        fromEntity.setNumber("FROM");
        fromEntity.setActualBalance(new BigDecimal("100.00"));
        fromEntity.setAvailableBalance(new BigDecimal("100.00"));
        BankAccountEntity toEntity = new BankAccountEntity();
        toEntity.setNumber("TO");
        toEntity.setActualBalance(new BigDecimal("25.00"));
        toEntity.setAvailableBalance(new BigDecimal("25.00"));
        when(bankAccountRepository.findByNumber("FROM")).thenReturn(Optional.of(fromEntity));
        when(bankAccountRepository.findByNumber("TO")).thenReturn(Optional.of(toEntity));

        service.internalFundTransfer(from, to, new BigDecimal("10.00"));

        assertThat(fromEntity.getActualBalance()).isEqualByComparingTo("90.00");
        assertThat(fromEntity.getAvailableBalance()).isEqualByComparingTo("90.00");
        assertThat(toEntity.getActualBalance()).isEqualByComparingTo("35.00");
        assertThat(toEntity.getAvailableBalance()).isEqualByComparingTo("35.00");
    }
}
