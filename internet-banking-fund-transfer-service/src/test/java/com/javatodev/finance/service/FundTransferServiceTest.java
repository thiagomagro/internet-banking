package com.javatodev.finance.service;

import com.javatodev.finance.model.TransactionStatus;
import com.javatodev.finance.model.dto.request.FundTransferRequest;
import com.javatodev.finance.model.dto.response.FundTransferResponse;
import com.javatodev.finance.model.entity.FundTransferEntity;
import com.javatodev.finance.model.repository.FundTransferRepository;
import com.javatodev.finance.service.rest.client.BankingCoreFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FundTransferServiceTest {

    @Test
    void successfulCoreResponseMovesRecordFromPendingToSuccess() {
        FundTransferRepository repository = mock(FundTransferRepository.class);
        BankingCoreFeignClient client = mock(BankingCoreFeignClient.class);
        FundTransferService service = new FundTransferService(repository, client);
        List<TransactionStatus> savedStatuses = new ArrayList<>();
        when(repository.save(any(FundTransferEntity.class))).thenAnswer(invocation -> {
            FundTransferEntity saved = invocation.getArgument(0);
            savedStatuses.add(saved.getStatus());
            return saved;
        });
        FundTransferResponse response = new FundTransferResponse();
        response.setTransactionId("core-transaction");
        when(client.fundTransfer(any())).thenReturn(response);

        FundTransferResponse result = service.fundTransfer(request());

        assertThat(result.getMessage()).isEqualTo("Fund Transfer Successfully Completed");
        verify(repository, times(2)).save(any(FundTransferEntity.class));
        assertThat(savedStatuses).containsExactly(TransactionStatus.PENDING, TransactionStatus.SUCCESS);
    }

    @Test
    void feignFailureLeavesThePersistedRecordPendingWithoutCompensation() {
        FundTransferRepository repository = mock(FundTransferRepository.class);
        BankingCoreFeignClient client = mock(BankingCoreFeignClient.class);
        FundTransferService service = new FundTransferService(repository, client);
        List<TransactionStatus> savedStatuses = new ArrayList<>();
        when(repository.save(any(FundTransferEntity.class))).thenAnswer(invocation -> {
            FundTransferEntity saved = invocation.getArgument(0);
            savedStatuses.add(saved.getStatus());
            return saved;
        });
        RuntimeException failure = new RuntimeException("core unavailable");
        when(client.fundTransfer(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.fundTransfer(request())).isSameAs(failure);

        verify(repository).save(any(FundTransferEntity.class));
        assertThat(savedStatuses).containsExactly(TransactionStatus.PENDING);
    }

    @Test
    void readsPagedTransfersThroughTheMapper() {
        FundTransferRepository repository = mock(FundTransferRepository.class);
        BankingCoreFeignClient client = mock(BankingCoreFeignClient.class);
        FundTransferService service = new FundTransferService(repository, client);
        FundTransferEntity entity = new FundTransferEntity();
        entity.setFromAccount("FROM");
        entity.setToAccount("TO");
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        assertThat(service.readAllTransfers(Pageable.unpaged()))
            .singleElement()
            .satisfies(transfer -> {
                assertThat(transfer.getFromAccount()).isEqualTo("FROM");
                assertThat(transfer.getToAccount()).isEqualTo("TO");
            });
    }

    private static FundTransferRequest request() {
        FundTransferRequest request = new FundTransferRequest();
        request.setFromAccount("FROM");
        request.setToAccount("TO");
        request.setAmount(new java.math.BigDecimal("10.00"));
        return request;
    }
}
