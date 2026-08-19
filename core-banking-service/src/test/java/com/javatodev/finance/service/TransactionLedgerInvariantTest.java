package com.javatodev.finance.service;

import com.javatodev.finance.model.TransactionType;
import com.javatodev.finance.model.dto.BankAccount;
import com.javatodev.finance.model.dto.UtilityAccount;
import com.javatodev.finance.model.dto.request.UtilityPaymentRequest;
import com.javatodev.finance.model.entity.BankAccountEntity;
import com.javatodev.finance.model.entity.TransactionEntity;
import com.javatodev.finance.repository.BankAccountRepository;
import com.javatodev.finance.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TransactionLedgerInvariantTest {

    @Test
    void transferCreatesOppositeEntriesWithOneTransactionIdAndMatchingBalances() {
        AccountService accountService = mock(AccountService.class);
        BankAccountRepository accounts = mock(BankAccountRepository.class);
        TransactionRepository transactions = mock(TransactionRepository.class);
        TransactionService service = new TransactionService(accountService, accounts, transactions);

        BankAccount from = account("FROM", "100.00");
        BankAccount to = account("TO", "25.00");
        BankAccountEntity fromEntity = entity("FROM", "100.00");
        BankAccountEntity toEntity = entity("TO", "25.00");
        when(accounts.findByNumber("FROM")).thenReturn(Optional.of(fromEntity));
        when(accounts.findByNumber("TO")).thenReturn(Optional.of(toEntity));

        service.internalFundTransfer(from, to, new BigDecimal("10.00"));

        ArgumentCaptor<TransactionEntity> entries = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactions, times(2)).save(entries.capture());
        assertThat(entries.getAllValues()).extracting(TransactionEntity::getTransactionType)
            .containsOnly(TransactionType.FUND_TRANSFER);
        assertThat(entries.getAllValues()).extracting(TransactionEntity::getTransactionId)
            .containsExactly(entries.getAllValues().get(0).getTransactionId(),
                entries.getAllValues().get(0).getTransactionId());
        assertThat(entries.getAllValues().stream().map(TransactionEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("0");
        assertThat(fromEntity.getActualBalance()).isEqualByComparingTo("90.00");
        assertThat(fromEntity.getAvailableBalance()).isEqualByComparingTo("90.00");
        assertThat(toEntity.getActualBalance()).isEqualByComparingTo("35.00");
        assertThat(toEntity.getAvailableBalance()).isEqualByComparingTo("35.00");
    }

    @Test
    void utilityPaymentKeepsAvailableBalanceInStepWithActualBalance() {
        AccountService accountService = mock(AccountService.class);
        BankAccountRepository accounts = mock(BankAccountRepository.class);
        TransactionRepository transactions = mock(TransactionRepository.class);
        TransactionService service = new TransactionService(accountService, accounts, transactions);

        when(accountService.readBankAccount("FROM")).thenReturn(account("FROM", "100.00"));
        when(accountService.readUtilityAccount(7L)).thenReturn(new UtilityAccount());
        BankAccountEntity entity = entity("FROM", "100.00");
        when(accounts.findByNumber("FROM")).thenReturn(Optional.of(entity));
        UtilityPaymentRequest request = new UtilityPaymentRequest();
        request.setAccount("FROM");
        request.setProviderId(7L);
        request.setAmount(new BigDecimal("12.50"));

        service.utilPayment(request);

        assertThat(entity.getActualBalance()).isEqualByComparingTo("87.50");
        assertThat(entity.getAvailableBalance()).isEqualByComparingTo("87.50");
        ArgumentCaptor<TransactionEntity> entry = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactions).save(entry.capture());
        assertThat(entry.getValue().getTransactionType()).isEqualTo(TransactionType.UTILITY_PAYMENT);
        assertThat(entry.getValue().getAmount()).isEqualByComparingTo("-12.50");
    }

    @Test
    void transferToTheSameAccountLeavesBalanceUnchangedButCreatesTwoEntries() {
        AccountService accountService = mock(AccountService.class);
        BankAccountRepository accounts = mock(BankAccountRepository.class);
        TransactionRepository transactions = mock(TransactionRepository.class);
        TransactionService service = new TransactionService(accountService, accounts, transactions);

        BankAccount account = account("SAME", "100.00");
        BankAccountEntity entity = entity("SAME", "100.00");
        when(accounts.findByNumber("SAME")).thenReturn(Optional.of(entity));

        service.internalFundTransfer(account, account, new BigDecimal("10.00"));

        assertThat(entity.getActualBalance()).isEqualByComparingTo("100.00");
        assertThat(entity.getAvailableBalance()).isEqualByComparingTo("100.00");
        verify(transactions, times(2)).save(any(TransactionEntity.class));
    }

    private static BankAccount account(String number, String balance) {
        BankAccount account = new BankAccount();
        account.setNumber(number);
        account.setActualBalance(new BigDecimal(balance));
        return account;
    }

    private static BankAccountEntity entity(String number, String balance) {
        BankAccountEntity entity = new BankAccountEntity();
        entity.setNumber(number);
        entity.setActualBalance(new BigDecimal(balance));
        entity.setAvailableBalance(new BigDecimal(balance));
        return entity;
    }
}
