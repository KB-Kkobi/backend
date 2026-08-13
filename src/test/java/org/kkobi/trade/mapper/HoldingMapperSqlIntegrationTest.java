package org.kkobi.trade.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.config.RootConfig;
import org.kkobi.product.mapper.ProductHoldingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * sumStockPrincipalByAccountId, sumActiveDepositByAccountId SQL 정확성 검증.
 * 실제 DB 에 삽입 후 쿼리 결과를 검증하며 @Transactional 로 자동 롤백된다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
@Transactional
class HoldingMapperSqlIntegrationTest {

    @Autowired
    private HoldingMapper holdingMapper;

    @Autowired
    private ProductHoldingMapper productHoldingMapper;

    private JdbcTemplate jdbc;

    @Autowired
    void setDataSource(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private Long createAccount() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "int-" + uid + "@test.com";
        jdbc.update(
                "INSERT INTO users (email, password, nickname, birth_date, postal_code, address_line1) " +
                "VALUES (?, ?, ?, '2000-01-01', '00000', '주소')",
                email, "pw", "닉-" + uid);
        Long userId = jdbc.queryForObject(
                "SELECT user_id FROM users WHERE email = ?", Long.class, email);
        jdbc.update(
                "INSERT INTO accounts (user_id, seed_money, cash_balance, locked_cash) " +
                        "VALUES (?, 1000000, 1000000, 0)",
                userId);
        return jdbc.queryForObject(
                "SELECT account_id FROM accounts WHERE user_id = ?", Long.class, userId);
    }

    private Long createSecurity() {
        String ticker = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        jdbc.update(
                "INSERT INTO securities (ticker, name, type, market, rt_score, lh_score, rp_score) " +
                "VALUES (?, ?, 'STOCK', 'KOSPI', 50.00, 50.00, 50.00)",
                ticker, "테스트-" + ticker);
        return jdbc.queryForObject(
                "SELECT security_id FROM securities WHERE ticker = ?", Long.class, ticker);
    }

    private void insertHolding(Long accountId, Long securityId, int quantity, long avgPrice) {
        jdbc.update(
                "INSERT INTO holding_securities (account_id, security_id, quantity, locked_quantity, average_price) " +
                "VALUES (?, ?, ?, 0, ?)",
                accountId, securityId, quantity, avgPrice);
    }

    private Long createProductOption() {
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        jdbc.update(
                "INSERT INTO products (product_type, fin_co_no, fin_prdt_cd, kor_co_nm, fin_prdt_nm) " +
                "VALUES ('DEPOSIT', 'FIN001', ?, '테스트은행', '테스트예금')",
                code);
        Long productId = jdbc.queryForObject(
                "SELECT product_id FROM products WHERE fin_prdt_cd = ?", Long.class, code);
        jdbc.update(
                "INSERT INTO product_options (product_id, intr_rate_type, intr_rate_type_nm, save_trm) " +
                "VALUES (?, 'S', '단리', 12)",
                productId);
        return jdbc.queryForObject(
                "SELECT product_option_id FROM product_options WHERE product_id = ?", Long.class, productId);
    }

    private void insertHoldingProduct(Long accountId, Long productOptionId, long joinAmount, String status) {
        jdbc.update(
                "INSERT INTO holding_products " +
                "(account_id, product_option_id, join_amount, applied_rate, start_date, maturity_date, status) " +
                "VALUES (?, ?, ?, 3.50, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 YEAR), ?)",
                accountId, productOptionId, joinAmount, status);
    }

    // ──────────────────────────────────────────────
    // sumStockPrincipalByAccountId 검증
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("보유 주식 없으면 sumStockPrincipal = 0 반환 (NULL 아님)")
    void sumStockPrincipal_returnsZero_whenNoHoldings() {
        Long accountId = createAccount();

        Long result = holdingMapper.sumStockPrincipalByAccountId(accountId);

        assertNotNull(result); assertEquals(0L, result);
    }

    @Test
    @DisplayName("average_price * quantity 합산이 정확하다")
    void sumStockPrincipal_calculatesSumCorrectly() {
        Long accountId = createAccount();
        Long sec1 = createSecurity();
        Long sec2 = createSecurity();

        // 10,000 * 3 = 30,000
        insertHolding(accountId, sec1, 3, 10_000L);
        // 5,000 * 5 = 25,000
        insertHolding(accountId, sec2, 5, 5_000L);

        Long result = holdingMapper.sumStockPrincipalByAccountId(accountId);

        assertEquals(55_000L, result);
    }

    @Test
    @DisplayName("다른 계좌의 보유 주식은 집계에서 제외된다")
    void sumStockPrincipal_excludesOtherAccount() {
        Long myAccount = createAccount();
        Long otherAccount = createAccount();
        Long sec = createSecurity();

        insertHolding(myAccount, sec, 2, 10_000L);   // 20,000

        Long mySecId2 = createSecurity();
        insertHolding(otherAccount, mySecId2, 10, 50_000L);  // 다른 계좌: 500,000

        Long result = holdingMapper.sumStockPrincipalByAccountId(myAccount);

        assertEquals(20_000L, result);
    }

    // ──────────────────────────────────────────────
    // sumActiveDepositByAccountId 검증
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("보유 예적금 없으면 sumActiveDeposit = 0 반환 (NULL 아님)")
    void sumActiveDeposit_returnsZero_whenNoProducts() {
        Long accountId = createAccount();

        Long result = productHoldingMapper.sumActiveDepositByAccountId(accountId);

        assertNotNull(result); assertEquals(0L, result);
    }

    @Test
    @DisplayName("ACTIVE 상태 join_amount 합산이 정확하다")
    void sumActiveDeposit_calculatesSumCorrectly() {
        Long accountId = createAccount();
        Long opt1 = createProductOption();
        Long opt2 = createProductOption();

        insertHoldingProduct(accountId, opt1, 500_000L, "ACTIVE");
        insertHoldingProduct(accountId, opt2, 300_000L, "ACTIVE");

        Long result = productHoldingMapper.sumActiveDepositByAccountId(accountId);

        assertEquals(800_000L, result);
    }

    @Test
    @DisplayName("TERMINATED 상태는 집계에서 제외된다")
    void sumActiveDeposit_excludesTerminatedStatus() {
        Long accountId = createAccount();
        Long activeOpt = createProductOption();
        Long termOpt = createProductOption();

        insertHoldingProduct(accountId, activeOpt, 1_000_000L, "ACTIVE");
        insertHoldingProduct(accountId, termOpt, 999_999L, "TERMINATED");

        Long result = productHoldingMapper.sumActiveDepositByAccountId(accountId);

        assertEquals(1_000_000L, result);
    }

    @Test
    @DisplayName("다른 계좌의 예적금은 집계에서 제외된다")
    void sumActiveDeposit_excludesOtherAccount() {
        Long myAccount = createAccount();
        Long otherAccount = createAccount();
        Long myOpt = createProductOption();
        Long otherOpt = createProductOption();

        insertHoldingProduct(myAccount, myOpt, 200_000L, "ACTIVE");
        insertHoldingProduct(otherAccount, otherOpt, 9_000_000L, "ACTIVE");

        Long result = productHoldingMapper.sumActiveDepositByAccountId(myAccount);

        assertEquals(200_000L, result);
    }
}
