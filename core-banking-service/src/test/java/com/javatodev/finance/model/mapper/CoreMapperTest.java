package com.javatodev.finance.model.mapper;

import com.javatodev.finance.model.AccountStatus;
import com.javatodev.finance.model.AccountType;
import com.javatodev.finance.model.dto.BankAccount;
import com.javatodev.finance.model.dto.User;
import com.javatodev.finance.model.dto.UtilityAccount;
import com.javatodev.finance.model.entity.BankAccountEntity;
import com.javatodev.finance.model.entity.UserEntity;
import com.javatodev.finance.model.entity.UtilityAccountEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CoreMapperTest {

    @Test
    void bankAccountMapperCopiesFieldsButNotNestedUser() {
        BankAccountEntity entity = new BankAccountEntity();
        entity.setId(1L);
        entity.setNumber("ACCOUNT");
        entity.setType(AccountType.SAVINGS_ACCOUNT);
        entity.setStatus(AccountStatus.ACTIVE);
        entity.setActualBalance(new BigDecimal("12.50"));
        entity.setAvailableBalance(new BigDecimal("10.00"));

        BankAccount dto = new BankAccountMapper().convertToDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNumber()).isEqualTo("ACCOUNT");
        assertThat(dto.getType()).isEqualTo(AccountType.SAVINGS_ACCOUNT);
        assertThat(dto.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(dto.getActualBalance()).isEqualByComparingTo("12.50");
        assertThat(dto.getAvailableBalance()).isEqualByComparingTo("10.00");
        assertThat(dto.getUser()).isNull();
    }

    @Test
    void userMapperMapsAccountsAndPreservesNullAccountCollection() {
        UserEntity entity = new UserEntity();
        entity.setIdentificationNumber("ID");
        entity.setFirstName("First");
        entity.setLastName("Last");
        entity.setAccounts(List.of());

        User dto = new UserMapper().convertToDto(entity);

        assertThat(dto.getIdentificationNumber()).isEqualTo("ID");
        assertThat(dto.getFirstName()).isEqualTo("First");
        assertThat(dto.getLastName()).isEqualTo("Last");
        assertThat(dto.getBankAccounts()).isEmpty();
    }

    @Test
    void mappersCopyFieldsInBothDirectionsAndSupportCollections() {
        BankAccount dto = new BankAccount();
        dto.setNumber("ACCOUNT");
        dto.setActualBalance(new BigDecimal("5.00"));
        dto.setAvailableBalance(new BigDecimal("4.00"));
        BankAccountEntity entity = new BankAccountMapper().convertToEntity(dto);
        assertThat(entity.getNumber()).isEqualTo("ACCOUNT");
        assertThat(entity.getActualBalance()).isEqualByComparingTo("5.00");

        User user = new User();
        user.setIdentificationNumber("ID");
        user.setBankAccounts(List.of(dto));
        UserEntity userEntity = new UserMapper().convertToEntity(user);
        assertThat(userEntity.getIdentificationNumber()).isEqualTo("ID");
        assertThat(userEntity.getAccounts()).hasSize(1);

        UtilityAccount utility = new UtilityAccount();
        utility.setProviderName("PROVIDER");
        UtilityAccountEntity utilityEntity = new UtilityAccountMapper().convertToEntity(utility);
        assertThat(utilityEntity.getProviderName()).isEqualTo("PROVIDER");
        assertThat(new UtilityAccountMapper().convertToDto(utilityEntity).getProviderName())
            .isEqualTo("PROVIDER");

        assertThat(new BankAccountMapper().convertToDtoList(List.of(entity))).hasSize(1);
        assertThat(new BankAccountMapper().convertToEntityList(List.of(dto))).hasSize(1);
        assertThat(new BankAccountMapper().convertToDtoSet(Set.of(entity))).hasSize(1);
        assertThat(new BankAccountMapper().convertToEntitySet(Set.of(dto))).hasSize(1);
    }

    @Test
    void nullInputsProduceEmptyDtoOrEntityWithoutThrowing() {
        assertThat(new BankAccountMapper().convertToDto((BankAccountEntity) null)).isNotNull();
        assertThat(new BankAccountMapper().convertToEntity((BankAccount) null)).isNotNull();
        assertThat(new UserMapper().convertToDto((UserEntity) null)).isNotNull();
        assertThat(new UserMapper().convertToEntity((User) null)).isNotNull();
        assertThat(new UtilityAccountMapper().convertToDto((UtilityAccountEntity) null)).isNotNull();
        assertThat(new UtilityAccountMapper().convertToEntity((UtilityAccount) null)).isNotNull();
    }
}
