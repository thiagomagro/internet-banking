package com.javatodev.finance.service;

import com.javatodev.finance.model.TransactionStatus;
import com.javatodev.finance.model.entity.UtilityPaymentEntity;
import com.javatodev.finance.model.rest.request.UtilityPaymentRequest;
import com.javatodev.finance.model.rest.response.UtilityPaymentResponse;
import com.javatodev.finance.repository.UtilityPaymentRepository;
import com.javatodev.finance.service.rest.BankingCoreRestClient;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UtilityPaymentServiceTest {

    @Test
    void successfulCoreResponseMovesRecordToSuccess() {
        UtilityPaymentRepository repository = mock(UtilityPaymentRepository.class);
        BankingCoreRestClient client = mock(BankingCoreRestClient.class);
        UtilityPaymentService service = new UtilityPaymentService(repository, client);
        List<TransactionStatus> savedStatuses = new ArrayList<>();
        when(repository.save(any(UtilityPaymentEntity.class))).thenAnswer(invocation -> {
            UtilityPaymentEntity saved = invocation.getArgument(0);
            savedStatuses.add(saved.getStatus());
            return saved;
        });
        when(client.utilityPayment(any())).thenReturn(UtilityPaymentResponse.builder().transactionId("tx-2").build());

        UtilityPaymentResponse result = service.utilPayment(request());

        assertThat(result.getMessage()).isEqualTo("Utility Payment Successfully Processed");
        verify(repository, times(2)).save(any(UtilityPaymentEntity.class));
        assertThat(savedStatuses).containsExactly(TransactionStatus.PROCESSING, TransactionStatus.SUCCESS);
    }

    @Test
    void coreFailureLeavesProcessingRecordWithoutCompensation() {
        UtilityPaymentRepository repository = mock(UtilityPaymentRepository.class);
        BankingCoreRestClient client = mock(BankingCoreRestClient.class);
        UtilityPaymentService service = new UtilityPaymentService(repository, client);
        List<TransactionStatus> savedStatuses = new ArrayList<>();
        when(repository.save(any(UtilityPaymentEntity.class))).thenAnswer(invocation -> {
            UtilityPaymentEntity saved = invocation.getArgument(0);
            savedStatuses.add(saved.getStatus());
            return saved;
        });
        RuntimeException failure = new RuntimeException("timeout");
        when(client.utilityPayment(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.utilPayment(request())).isSameAs(failure);

        verify(repository).save(any(UtilityPaymentEntity.class));
        assertThat(savedStatuses).containsExactly(TransactionStatus.PROCESSING);
    }

    @Test
    void readsPagedPaymentsThroughTheMapper() {
        UtilityPaymentRepository repository = mock(UtilityPaymentRepository.class);
        BankingCoreRestClient client = mock(BankingCoreRestClient.class);
        UtilityPaymentService service = new UtilityPaymentService(repository, client);
        UtilityPaymentEntity entity = new UtilityPaymentEntity();
        entity.setAccount("ACCOUNT");
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        assertThat(service.readPayments(Pageable.unpaged()))
            .singleElement()
            .satisfies(payment -> assertThat(payment.getAccount()).isEqualTo("ACCOUNT"));
    }

    private static UtilityPaymentRequest request() {
        UtilityPaymentRequest request = new UtilityPaymentRequest();
        request.setAccount("ACCOUNT");
        request.setProviderId(1L);
        request.setAmount(new java.math.BigDecimal("5.00"));
        request.setReferenceNumber("REF");
        return request;
    }
}
