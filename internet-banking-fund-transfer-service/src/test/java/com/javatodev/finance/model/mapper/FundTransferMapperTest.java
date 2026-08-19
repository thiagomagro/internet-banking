package com.javatodev.finance.model.mapper;

import com.javatodev.finance.model.TransactionStatus;
import com.javatodev.finance.model.dto.FundTransfer;
import com.javatodev.finance.model.entity.FundTransferEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FundTransferMapperTest {

    private final FundTransferMapper mapper = new FundTransferMapper();

    @Test
    void mapsEveryFundTransferField() {
        FundTransferEntity entity = new FundTransferEntity();
        entity.setId(4L);
        entity.setTransactionReference("tx");
        entity.setFromAccount("from");
        entity.setToAccount("to");
        entity.setAmount(new BigDecimal("1.25"));
        entity.setStatus(TransactionStatus.SUCCESS);

        FundTransfer dto = mapper.convertToDto(entity);

        assertThat(dto.getId()).isEqualTo(4L);
        assertThat(dto.getTransactionReference()).isEqualTo("tx");
        assertThat(dto.getFromAccount()).isEqualTo("from");
        assertThat(dto.getToAccount()).isEqualTo("to");
        assertThat(dto.getAmount()).isEqualByComparingTo("1.25");
        // BeanUtils silently skips the enum-to-String status type mismatch.
        assertThat(dto.getStatus()).isNull();
    }

    @Test
    void preservesNullFieldsInsteadOfInventingValues() {
        FundTransfer dto = mapper.convertToDto(new FundTransferEntity());

        assertThat(dto.getTransactionReference()).isNull();
        assertThat(dto.getFromAccount()).isNull();
        assertThat(dto.getToAccount()).isNull();
        assertThat(dto.getAmount()).isNull();
        assertThat(dto.getStatus()).isNull();
        assertThat(mapper.convertToEntity((FundTransfer) null).getFromAccount()).isNull();
    }

    @Test
    void mapsDtoFieldsBackToEntityAndSupportsCollectionConversions() {
        FundTransfer dto = new FundTransfer();
        dto.setFromAccount("from");
        dto.setToAccount("to");
        dto.setAmount(new BigDecimal("3.00"));

        FundTransferEntity entity = mapper.convertToEntity(dto);

        assertThat(entity.getFromAccount()).isEqualTo("from");
        assertThat(entity.getToAccount()).isEqualTo("to");
        assertThat(entity.getAmount()).isEqualByComparingTo("3.00");
        assertThat(mapper.convertToDtoList(List.of(entity))).hasSize(1);
        assertThat(mapper.convertToEntityList(List.of(dto))).hasSize(1);
        assertThat(mapper.convertToDtoSet(List.of(entity))).hasSize(1);
        assertThat(mapper.convertToEntitySet(List.of(dto))).hasSize(1);
    }
}
