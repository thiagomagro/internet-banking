package com.javatodev.finance;

import com.javatodev.finance.fixture.BankAccountEntityBuilder;
import com.javatodev.finance.fixture.UserEntityBuilder;
import com.javatodev.finance.fixture.UtilityAccountEntityBuilder;
import com.javatodev.finance.model.TransactionType;
import com.javatodev.finance.model.dto.request.FundTransferRequest;
import com.javatodev.finance.model.dto.request.UtilityPaymentRequest;
import com.javatodev.finance.model.entity.BankAccountEntity;
import com.javatodev.finance.model.entity.TransactionEntity;
import com.javatodev.finance.model.entity.UserEntity;
import com.javatodev.finance.repository.BankAccountRepository;
import com.javatodev.finance.repository.TransactionRepository;
import com.javatodev.finance.repository.UserRepository;
import com.javatodev.finance.repository.UtilityAccountRepository;
import com.javatodev.finance.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.concurrent.Future;

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
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanData() {
        transactionRepository.deleteAllInBatch();
        bankAccountRepository.deleteAllInBatch();
        utilityAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void migratedMySqlSchemaPassesHibernateValidationAndFlywayHistoryIsApplied() {
        Integer migrationCount = jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where success = true",
            Integer.class
        );

        assertThat(migrationCount).isEqualTo(3);
        assertThat(bankAccountRepository.count()).isZero();
    }

    @Test
    @Transactional
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
        transactionRepository.save(TransactionEntity.builder()
            .account(account)
            .transactionId("1")
            .referenceNumber("TEST")
            .transactionType(TransactionType.FUND_TRANSFER)
            .amount(new BigDecimal("10.00"))
            .build());
        transactionRepository.save(TransactionEntity.builder()
            .account(account)
            .transactionId("2")
            .referenceNumber("TEST")
            .transactionType(TransactionType.FUND_TRANSFER)
            .amount(new BigDecimal("-5.00"))
            .build());

        assertThat(bankAccountRepository.findByNumber("ACCOUNT-1"))
            .get()
            .extracting(BankAccountEntity::getNumber)
            .isEqualTo("ACCOUNT-1");
        Page<TransactionEntity> page = transactionRepository.findAll(
            PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "id"))
        );
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
            .extracting(TransactionEntity::getTransactionId)
            .containsExactly("1");
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
        var utilityAccount = utilityAccountRepository.save(
            UtilityAccountEntityBuilder.aUtilityAccount()
                .withNumber("UTILITY-1")
                .withProviderName("PROVIDER-1")
                .build()
        );

        UtilityPaymentRequest request = new UtilityPaymentRequest();
        request.setAccount("ACCOUNT-IDEMPOTENCY");
        request.setProviderId(utilityAccount.getId());
        request.setAmount(new BigDecimal("10.00"));
        request.setReferenceNumber("SAME-REFERENCE");

        transactionService.utilPayment(request);
        transactionService.utilPayment(request);

        Integer transactionsWithReference = jdbcTemplate.queryForObject(
            "select count(*) from banking_core_transaction where reference_number = ?",
            Integer.class,
            "SAME-REFERENCE"
        );
        assertThat(transactionsWithReference).isEqualTo(2);
    }

    /**
     * Two service transactions race to transfer 60.00 from a 100.00 source account.
     * Across five MySQL runs, the first thread raised CannotAcquireLockException
     * caused by a MySQL deadlock and the second thread committed. The observed
     * balances were source 40.00 and destination 60.00, with two ledger entries
     * summing to 0.00. This is an infrastructure failure for a legitimate request,
     * not a lost-update observation.
     */
    @Test
    @Tag("known-gap")
    void concurrentTransfersExposeDeadlockForOneLegitimateRequest() throws Exception {
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
        try {
            var first = executor.submit(() -> {
                start.await();
                return transactionService.fundTransfer(request);
            });
            var second = executor.submit(() -> {
                start.await();
                return transactionService.fundTransfer(request);
            });
            start.countDown();
            TransferOutcome firstOutcome = awaitTransfer("first", first);
            TransferOutcome secondOutcome = awaitTransfer("second", second);

            System.out.println("concurrency outcome: " + firstOutcome);
            System.out.println("concurrency outcome: " + secondOutcome);

            assertThat(List.of(firstOutcome, secondOutcome))
                .extracting(TransferOutcome::outcome)
                .containsExactlyInAnyOrder("COMMITTED", "EXCEPTION");
            TransferOutcome failure = List.of(firstOutcome, secondOutcome).stream()
                .filter(outcome -> "EXCEPTION".equals(outcome.outcome()))
                .findFirst()
                .orElseThrow();
            assertThat(failure.exceptionType()).isEqualTo(CannotAcquireLockException.class.getName());
            assertThat(failure.exceptionChain()).contains("Deadlock found when trying to get lock");
        } finally {
            executor.shutdownNow();
        }

        BigDecimal sourceBalance = jdbcTemplate.queryForObject(
            "select actual_balance from banking_core_account where number = ?",
            BigDecimal.class,
            "ACCOUNT-SOURCE"
        );
        BigDecimal destinationBalance = jdbcTemplate.queryForObject(
            "select actual_balance from banking_core_account where number = ?",
            BigDecimal.class,
            "ACCOUNT-DESTINATION"
        );
        Integer entryCount = jdbcTemplate.queryForObject(
            "select count(*) from banking_core_transaction",
            Integer.class
        );
        BigDecimal ledgerSum = jdbcTemplate.queryForObject(
            "select coalesce(sum(amount), 0) from banking_core_transaction",
            BigDecimal.class
        );

        assertThat(sourceBalance).isEqualByComparingTo("40.00");
        assertThat(destinationBalance).isEqualByComparingTo("60.00");
        assertThat(entryCount).isEqualTo(2);
        assertThat(ledgerSum).isEqualByComparingTo("0.00");
    }

    private static TransferOutcome awaitTransfer(String name, Future<?> transfer) {
        try {
            transfer.get();
            return new TransferOutcome(name, "COMMITTED", null, null);
        } catch (java.util.concurrent.ExecutionException exception) {
            Throwable failure = exception.getCause();
            return new TransferOutcome(
                name,
                "EXCEPTION",
                failure.getClass().getName(),
                describeCauseChain(failure)
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while awaiting concurrent transfer", exception);
        }
    }

    private static String describeCauseChain(Throwable failure) {
        StringBuilder description = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (description.length() > 0) {
                description.append(" <- ");
            }
            description
                .append(current.getClass().getName())
                .append(": ")
                .append(String.valueOf(current.getMessage()).replace('\n', ' '));
        }
        return description.toString();
    }

    private record TransferOutcome(
        String thread,
        String outcome,
        String exceptionType,
        String exceptionChain
    ) {
    }
}
