package com.drippay.integration;

import com.drippay.domain.enums.LedgerEntryType;
import com.drippay.repository.LedgerEntryRepository;
import com.drippay.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MySQL-backed integration tests (requires local MySQL on localhost:3306, user root, empty password).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class DripPayFinancialIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired WalletRepository walletRepository;
    @Autowired LedgerEntryRepository ledgerEntryRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private String userAToken;
    private String userBToken;
    private String userAPublicWallet;
    private String userBPublicWallet;
    @BeforeAll
    void assumeMysql() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception ex) {
            Assumptions.abort("Local MySQL not available: " + ex.getMessage());
        }
    }

    @BeforeEach
    void registerUsers() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        userAToken = registerAndLogin("qa_a_" + suffix + "@drippay.test", "Password123!");
        userBToken = registerAndLogin("qa_b_" + suffix + "@drippay.test", "Password123!");

        userAPublicWallet = fetchPublicWalletId(userAToken);
        userBPublicWallet = fetchPublicWalletId(userBToken);
    }

    @Test
    @DisplayName("IT-AUTH-01 duplicate email rejected")
    void duplicateEmailRejected() throws Exception {
        String body = """
                {"email":"dup@test.com","password":"Password123!","fullName":"Dup"}
                """;
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("IT-AUTH-03 refresh rotates token")
    void refreshRotatesToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"qa_a_refresh@drippay.test","password":"Password123!","fullName":"QA"}
                                """))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"qa_a_refresh@drippay.test","password":"Password123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String refresh1 = objectMapper.readTree(login.getResponse().getContentAsString()).get("refreshToken").asText();
        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh1 + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String refresh2 = objectMapper.readTree(refreshed.getResponse().getContentAsString()).get("refreshToken").asText();
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh1 + "\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(refresh2).isNotEqualTo(refresh1);
    }

    @Test
    @DisplayName("IT-IDEM-03 missing idempotency key returns 400")
    void missingIdempotencyKey() throws Exception {
        deposit(userAToken, 100);
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientWalletId":"%s","amount":10}
                                """.formatted(userBPublicWallet)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IT-AUTH-02 invalid login returns 401")
    void invalidLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nope@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("IT-WAL-01 demo deposit increases balance")
    void demoDeposit() throws Exception {
        BigDecimal before = fetchBalance(userAToken);
        mockMvc.perform(post("/api/v1/wallets/demo-deposits")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500}"))
                .andExpect(status().isOk());
        BigDecimal after = fetchBalance(userAToken);
        assertThat(after).isEqualByComparingTo(before.add(new BigDecimal("500.0000")));
    }

    @Test
    @DisplayName("IT-TX-01 transfer debits sender and credits recipient")
    void transferBalances() throws Exception {
        deposit(userAToken, 1000);
        BigDecimal senderBefore = fetchBalance(userAToken);
        BigDecimal recipientBefore = fetchBalance(userBToken);

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userAToken)
                        .header("Idempotency-Key", "it-key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientWalletId":"%s","amount":250,"note":"QA transfer"}
                                """.formatted(userBPublicWallet)))
                .andExpect(status().isOk());

        assertThat(fetchBalance(userAToken)).isEqualByComparingTo(senderBefore.subtract(new BigDecimal("250.0000")));
        assertThat(fetchBalance(userBToken)).isEqualByComparingTo(recipientBefore.add(new BigDecimal("250.0000")));
    }

    @Test
    @DisplayName("IT-TX-02 ledger debits equal credits for transfer")
    void ledgerBalanced() throws Exception {
        deposit(userAToken, 300);
        MvcResult result = mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userAToken)
                        .header("Idempotency-Key", "ledger-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientWalletId":"%s","amount":100}
                                """.formatted(userBPublicWallet)))
                .andExpect(status().isOk())
                .andReturn();

        Long txId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        var entries = ledgerEntryRepository.findByTransactionId(txId);
        assertThat(entries).hasSize(2);

        BigDecimal debits = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.DEBIT)
                .map(e -> e.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.CREDIT)
                .map(e -> e.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debits).isEqualByComparingTo(credits);
        assertThat(debits).isEqualByComparingTo("100.0000");
    }

    @Test
    @DisplayName("IT-TX-03 idempotent transfer does not double-charge")
    void idempotency() throws Exception {
        deposit(userAToken, 500);
        String key = "same-key-" + UUID.randomUUID();
        String payload = """
                {"recipientWalletId":"%s","amount":50,"note":"idem"}
                """.formatted(userBPublicWallet);

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userAToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        BigDecimal afterFirst = fetchBalance(userAToken);

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userAToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        assertThat(fetchBalance(userAToken)).isEqualByComparingTo(afterFirst);
    }

    @Test
    @DisplayName("IT-TX-04 idempotency key conflict returns 409")
    void idempotencyConflict() throws Exception {
        deposit(userAToken, 200);
        String key = "conflict-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userAToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientWalletId":"%s","amount":10}
                                """.formatted(userBPublicWallet)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userAToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientWalletId":"%s","amount":20}
                                """.formatted(userBPublicWallet)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("IT-TX-05 insufficient balance rejected")
    void insufficientBalance() throws Exception {
        deposit(userAToken, 50);
        BigDecimal before = fetchBalance(userAToken);
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userAToken)
                        .header("Idempotency-Key", "insuf-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientWalletId":"%s","amount":500}
                                """.formatted(userBPublicWallet)))
                .andExpect(status().isBadRequest());
        assertThat(fetchBalance(userAToken)).isEqualByComparingTo(before);
    }

    @Test
    @DisplayName("IT-TX-06 self-transfer rejected")
    void selfTransfer() throws Exception {
        deposit(userAToken, 100);
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userAToken)
                        .header("Idempotency-Key", "self-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientWalletId":"%s","amount":10}
                                """.formatted(userAPublicWallet)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IT-SEC-01 user cannot read another user's transaction")
    void transactionAccessControl() throws Exception {
        deposit(userAToken, 100);
        MvcResult tx = mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userAToken)
                        .header("Idempotency-Key", "acl-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientWalletId":"%s","amount":10}
                                """.formatted(userBPublicWallet)))
                .andExpect(status().isOk())
                .andReturn();
        long txId = objectMapper.readTree(tx.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/transactions/" + txId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk());

        String userCToken = registerAndLogin("qa_c_" + UUID.randomUUID().toString().substring(0, 8) + "@drippay.test", "Password123!");
        mockMvc.perform(get("/api/v1/transactions/" + txId)
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT-SEC-02 admin endpoint forbidden for regular user")
    void adminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/alerts")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT-TX-07 concurrent transfers cannot overdraw wallet")
    void concurrentTransfers() throws Exception {
        deposit(userAToken, 100);
        BigDecimal before = fetchBalance(userAToken);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    int status = mockMvc.perform(post("/api/v1/transfers")
                                    .header("Authorization", "Bearer " + userAToken)
                                    .header("Idempotency-Key", "conc-" + UUID.randomUUID())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"recipientWalletId":"%s","amount":80}
                                            """.formatted(userBPublicWallet)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                    if (status == 200) success.incrementAndGet();
                    else failed.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        pool.shutdown();

        BigDecimal after = fetchBalance(userAToken);
        assertThat(after).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(after).isEqualByComparingTo(before.subtract(new BigDecimal("80.0000")));
        assertThat(success.get()).isEqualTo(1);
        assertThat(failed.get()).isEqualTo(1);
    }

    private String registerAndLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","fullName":"QA User"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String fetchPublicWalletId(String token) throws Exception {
        MvcResult me = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(me.getResponse().getContentAsString()).get("publicWalletId").asText();
    }

    private BigDecimal fetchBalance(String token) throws Exception {
        MvcResult wallet = mockMvc.perform(get("/api/v1/wallets/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode balance = objectMapper.readTree(wallet.getResponse().getContentAsString()).get("balance");
        return balance.decimalValue();
    }

    private void deposit(String token, int amount) throws Exception {
        mockMvc.perform(post("/api/v1/wallets/demo-deposits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + "}"))
                .andExpect(status().isOk());
    }
}
