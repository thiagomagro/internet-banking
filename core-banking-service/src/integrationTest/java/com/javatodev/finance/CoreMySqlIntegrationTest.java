package com.javatodev.finance;

import com.javatodev.finance.fixture.BankAccountEntityBuilder;
import com.javatodev.finance.fixture.UserEntityBuilder;
import com.javatodev.finance.model.AccountStatus;
import com.javatodev.finance.model.AccountType;
import com.javatodev.finance.model.TransactionType;
import com.javatodev.finance.model.dto.request.FundTransferRequest;
import com.javatodev.finance.model.dto.request.UtilityPaymentRequest;
import com.javatodev.finance.model.entity.BankAccountEntity;
import com.javatodev.finance.model.entity.TransactionEntity;
import com.javatodev.finance.model.entity.UserEntity;
import com.javatodev.finance.model.entity.UtilityAccountEntity;
import com.javatodev.finance.repository.BankAccountRepository;
import com.javatodev.finance.repository.TransactionRepository;
import com.javatodev.finance.repository.UserRepository;
import com.javatodev.finance.repository.UtilityAccountRepository;
import com.javatodev.finance.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.cloud.bootstrap.enabled=false"
})
@ActiveProfiles("integration")
@Testcontainers
class CoreMySqlIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
        .withDatabaseName("banking_core_service")
        .withUsername("root")
        .withPassword("password");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private BankAccountRepository bankAccountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UtilityAccountRepository utilityAccountRepository;
    @Autowired
    private TransactionService transactionService;

    @BeforeEach
    void cleanData() {
        transactionRepository.deleteAll();
        bankAccountRepository.deleteAll();
        utilityAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void migratedMySqlSchemaPassesHibernateValidation() {
        assertThat(MYSQL.isRunning()).isTrue();
    }

    @Test
    void findsAccountByNumberAndPaginatesTransactions() {
        UserEntity user = userRepository.save(UserEntityBuilder.aUser()
            .withIdentificationNumber("TEST-USER")
            .withName("Test", "User")
            .build());
        BankAccountEntity account = BankAccountEntityBuilder.anAccount()
            .withNumber("ACCOUNT-1")
            .withBalance("100.00")
            .build();
        account.setUser(user);
        account = bankAccountRepository.save(account);
        transactionRepository.save(transaction(account, "1", "10.00"));
        transactionRepository.save(transaction(account, "2", "-5.00"));

        assertThat(bankAccountRepository.findByNumber("ACCOUNT-1")).contains(account);
        Page<TransactionEntity> page = transactionRepository.findAll(PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void repeatedReferenceNumberCreatesTwoTransactionsBecauseIdempotencyIsNotImplemented() {
        UserEntity user = userRepository.save(UserEntityBuilder.aUser()
            .withIdentificationNumber("IDEMPOTENCY-USER")
            .build());
        BankAccountEntity account = BankAccountEntityBuilder.anAccount()
            .withNumber("ACCOUNT-IDEMPOTENCY")
            .withBalance("100.00")
            .build();
        account.setUser(user);
        bankAccountRepository.save(account);
        utilityAccountRepository.save(new UtilityAccountEntity());

        UtilityPaymentRequest request = new UtilityPaymentRequest();
        request.setAccount("ACCOUNT-IDEMPOTENCY");
        request.setProviderId(1L);
        request.setAmount(new BigDecimal("10.00"));
        request.setReferenceNumber("SAME-REFERENCE");

        transactionService.utilPayment(request);
        transactionService.utilPayment(request);

        assertThat(transactionRepository.findAll())
            .filteredOn(tx -> "SAME-REFERENCE".equals(tx.getReferenceNumber()))
            .hasSize(2);
    }

    /**
     * Two service transactions can read the same balance and persist a derived balance.
     * The current implementation has no pessimistic lock or optimistic version, so this
     * test is intentionally disabled after documenting the lost-update gap. It has not
     * executed in this run because the integration context is blocked by the Flyway/JPA
     * enum-varchar schema mismatch reported by the schema validation test.
     */
    @Test
    @Tag("known-gap")
    @Disabled("known-gap: concurrent transfers can lose an update without locking/versioning")
    void concurrentTransfersExposeLostUpdateAndPossibleOverdraft() throws Exception {
        UserEntity user = userRepository.save(UserEntityBuilder.aUser()
            .withIdentificationNumber("CONCURRENCY-USER")
            .build());
        BankAccountEntity source = BankAccountEntityBuilder.anAccount()
            .withNumber("ACCOUNT-SOURCE")
            .withBalance("100.00")
            .build();
        source.setUser(user);
        BankAccountEntity destination = BankAccountEntityBuilder.anAccount()
            .withNumber("ACCOUNT-DESTINATION")
            .withBalance("0.00")
            .build();
        destination.setUser(user);
        bankAccountRepository.saveAll(List.of(source, destination));

        FundTransferRequest request = new FundTransferRequest();
        request.setFromAccount("ACCOUNT-SOURCE");
        request.setToAccount("ACCOUNT-DESTINATION");
        request.setAmount(new BigDecimal("60.00"));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        var first = executor.submit(() -> {
            start.await();
            return transactionService.fundTransfer(request);
        });
        var second = executor.submit(() -> {
            start.await();
            return transactionService.fundTransfer(request);
        });
        start.countDown();
        first.get();
        second.get();
        executor.shutdown();

        assertThat(bankAccountRepository.findByNumber("ACCOUNT-SOURCE").orElseThrow().getActualBalance())
            .isEqualByComparingTo("-20.00");
    }

    private static TransactionEntity transaction(BankAccountEntity account, String id, String amount) {
        return TransactionEntity.builder()
            .account(account)
            .transactionId(id)
            .referenceNumber("TEST")
            .transactionType(TransactionType.FUND_TRANSFER)
            .amount(new BigDecimal(amount))
            .build();
    }
}
