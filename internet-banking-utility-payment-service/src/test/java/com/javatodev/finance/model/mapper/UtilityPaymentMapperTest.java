package com.javatodev.finance.model.mapper;

import com.javatodev.finance.model.TransactionStatus;
import com.javatodev.finance.model.dto.UtilityPayment;
import com.javatodev.finance.model.entity.UtilityPaymentEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UtilityPaymentMapperTest {

    private final UtilityPaymentMapper mapper = new UtilityPaymentMapper();

    @Test
    void mapsEveryUtilityPaymentField() {
        UtilityPaymentEntity entity = new UtilityPaymentEntity();
        entity.setId(8L);
        entity.setProviderId(2L);
        entity.setAmount(new BigDecimal("8.50"));
        entity.setReferenceNumber("ref");
        entity.setAccount("account");
        entity.setTransactionId("tx");
        entity.setStatus(TransactionStatus.SUCCESS);

        UtilityPayment dto = mapper.convertToDto(entity);

        assertThat(dto.getProviderId()).isEqualTo(2L);
        assertThat(dto.getAmount()).isEqualByComparingTo("8.50");
        assertThat(dto.getReferenceNumber()).isEqualTo("ref");
        assertThat(dto.getAccount()).isEqualTo("account");
        assertThat(dto.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    void mapsDtoFieldsBackToEntityAndSupportsCollectionConversions() {
        UtilityPayment dto = new UtilityPayment();
        dto.setProviderId(3L);
        dto.setAmount(new BigDecimal("3.00"));
        dto.setReferenceNumber("ref");
        dto.setAccount("account");

        UtilityPaymentEntity entity = mapper.convertToEntity(dto);

        assertThat(entity.getProviderId()).isEqualTo(3L);
        assertThat(entity.getAmount()).isEqualByComparingTo("3.00");
        assertThat(entity.getReferenceNumber()).isEqualTo("ref");
        assertThat(entity.getAccount()).isEqualTo("account");
        assertThat(mapper.convertToDtoList(List.of(entity))).hasSize(1);
        assertThat(mapper.convertToEntityList(List.of(dto))).hasSize(1);
        assertThat(mapper.convertToDtoSet(List.of(entity))).hasSize(1);
        assertThat(mapper.convertToEntitySet(List.of(dto))).hasSize(1);
        assertThat(mapper.convertToEntity((UtilityPayment) null).getAccount()).isNull();
    }
}
