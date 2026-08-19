package com.javatodev.finance.model.mapper;

import com.javatodev.finance.model.dto.FundTransfer;
import com.javatodev.finance.model.entity.FundTransferEntity;
import com.javatodev.finance.model.TransactionStatus;

import org.springframework.beans.BeanUtils;

public class FundTransferMapper extends BaseMapper<FundTransferEntity, FundTransfer> {
    @Override
    public FundTransferEntity convertToEntity(FundTransfer dto, Object... args) {
        FundTransferEntity entity = new FundTransferEntity();
        if (dto != null) {
            BeanUtils.copyProperties(dto, entity);
            if (dto.getStatus() != null) {
                entity.setStatus(TransactionStatus.valueOf(dto.getStatus()));
            }
        }
        return entity;
    }

    @Override
    public FundTransfer convertToDto(FundTransferEntity entity, Object... args) {
        FundTransfer dto = new FundTransfer();
        if (entity != null) {
            BeanUtils.copyProperties(entity, dto);
            if (entity.getStatus() != null) {
                dto.setStatus(entity.getStatus().name());
            }
        }
        return dto;
    }
}
