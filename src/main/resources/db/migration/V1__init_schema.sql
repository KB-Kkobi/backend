-- =========================================================
-- 1. 회원
-- PK: user_id → 테이블명 users
-- =========================================================

CREATE TABLE users (
                       user_id BIGINT NOT NULL AUTO_INCREMENT,
                       email VARCHAR(100) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       nickname VARCHAR(30) NOT NULL,
                       birth_date DATE NOT NULL,
                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       postal_code VARCHAR(10) NOT NULL,
                       address_line1 VARCHAR(255) NOT NULL,
                       address_line2 VARCHAR(255) NULL,

                       PRIMARY KEY (user_id),
                       UNIQUE (email),
                       UNIQUE (nickname)
);


-- =========================================================
-- 2. 토큰 로그
-- PK: log_id → 테이블명 logs
-- =========================================================

CREATE TABLE logs (
                      log_id BIGINT NOT NULL AUTO_INCREMENT,
                      user_id BIGINT NOT NULL,
                      token_hash CHAR(64) NOT NULL,
                      token_created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      event_type ENUM(
        'ISSUED',
        'REFRESHED',
        'REVOKED',
        'REUSE_DETECTED'
    ) NOT NULL,
                      token_expires_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                      PRIMARY KEY (log_id),

                      FOREIGN KEY (user_id)
                          REFERENCES users(user_id)
);


-- =========================================================
-- 3. 투자 성향
-- PK: persona_id → 테이블명 personas
-- =========================================================

CREATE TABLE personas (
                          persona_id BIGINT NOT NULL AUTO_INCREMENT,
                          persona_name VARCHAR(50) NOT NULL,
                          description TEXT NULL,
                          feature TEXT NULL,
                          strength TEXT NULL,
                          caution TEXT NULL,
                          stock_ratio DECIMAL(5,2) NOT NULL DEFAULT 0,
                          bond_ratio DECIMAL(5,2) NOT NULL DEFAULT 0,
                          deposit_ratio DECIMAL(5,2) NOT NULL DEFAULT 0,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          PRIMARY KEY (persona_id),

                          CHECK (stock_ratio BETWEEN 0 AND 100),
                          CHECK (bond_ratio BETWEEN 0 AND 100),
                          CHECK (deposit_ratio BETWEEN 0 AND 100),

                          CHECK (
                              stock_ratio + bond_ratio + deposit_ratio = 100
                              )
);


-- =========================================================
-- 4. 이벤트
-- PK: event_id → 테이블명 events
-- =========================================================

CREATE TABLE events (
                        event_id BIGINT NOT NULL AUTO_INCREMENT,
                        event_seq INT NOT NULL,
                        trigger_tick INT NOT NULL,
                        title VARCHAR(100) NOT NULL,
                        description TEXT NOT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        PRIMARY KEY (event_id),

                        UNIQUE (event_seq),

                        CHECK (event_seq > 0),
                        CHECK (trigger_tick >= 0)
);


-- =========================================================
-- 5. 행동 로그
-- PK: action_log_id → 테이블명 action_logs
-- =========================================================

CREATE TABLE action_logs (
                             action_log_id BIGINT NOT NULL AUTO_INCREMENT,
                             user_id BIGINT NOT NULL,
                             game_month TINYINT NOT NULL,
                             action_type VARCHAR(20) NOT NULL,
                             asset_type VARCHAR(20) NOT NULL,
                             action_amount BIGINT NULL,
                             market_state VARCHAR(20) NOT NULL,
                             deposit_status VARCHAR(20) NOT NULL,
                             current_cash BIGINT NOT NULL,
                             current_stock BIGINT NOT NULL,
                             current_deposit BIGINT NOT NULL,
                             rt_score_delta DECIMAL(5,2) NOT NULL,
                             lh_score_delta DECIMAL(5,2) NOT NULL,
                             rp_score_delta DECIMAL(5,2) NOT NULL,
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             PRIMARY KEY (action_log_id),

                             FOREIGN KEY (user_id)
                                 REFERENCES users(user_id),

                             CHECK (
                                 game_month BETWEEN 1 AND 12
                                 ),

                             CHECK (
                                 action_type IN (
                                                 'INITIAL_ALLOCATION',
                                                 'BUY',
                                                 'SELL',
                                                 'DEPOSIT_CANCEL'
                                     )
                                 ),

                             CHECK (
                                 asset_type IN (
                                                'ALL',
                                                'STOCK',
                                                'DEPOSIT',
                                                'CASH'
                                     )
                                 ),

                             CHECK (
                                 action_amount IS NULL
                                     OR action_amount >= 0
                                 ),

                             CHECK (
                                 market_state IN (
                                                  'BULL',
                                                  'BEAR',
                                                  'SIDEWAYS',
                                                  'CRASH',
                                                  'VOLATILE'
                                     )
                                 ),

                             CHECK (
                                 deposit_status IN (
                                                    'NONE',
                                                    'ACTIVE',
                                                    'MATURED',
                                                    'CANCELLED'
                                     )
                                 ),

                             CHECK (current_cash >= 0),
                             CHECK (current_stock >= 0),
                             CHECK (current_deposit >= 0),

                             CHECK (
                                 rt_score_delta BETWEEN -100 AND 100
                                 ),

                             CHECK (
                                 lh_score_delta BETWEEN -100 AND 100
                                 ),

                             CHECK (
                                 rp_score_delta BETWEEN -100 AND 100
                                 )
);


-- =========================================================
-- 6. 성향 분석 결과
-- PK: result_id → 테이블명 results
-- =========================================================

CREATE TABLE results (
                         result_id BIGINT NOT NULL AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,
                         persona_id BIGINT NOT NULL,
                         rt_score DECIMAL(5,2) NOT NULL,
                         lh_score DECIMAL(5,2) NOT NULL,
                         rp_score DECIMAL(5,2) NOT NULL,
                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         PRIMARY KEY (result_id),

                         INDEX (user_id),
                         INDEX (persona_id),

                         FOREIGN KEY (user_id)
                             REFERENCES users(user_id),

                         FOREIGN KEY (persona_id)
                             REFERENCES personas(persona_id),

                         CHECK (
                             rt_score BETWEEN 0 AND 100
                             ),

                         CHECK (
                             lh_score BETWEEN 0 AND 100
                             ),

                         CHECK (
                             rp_score BETWEEN 0 AND 100
                             )
);


-- =========================================================
-- 7. 계좌
-- PK: account_id → 테이블명 accounts
-- =========================================================

CREATE TABLE accounts (
                          account_id BIGINT NOT NULL AUTO_INCREMENT,
                          user_id BIGINT NOT NULL,
                          seed_money BIGINT NOT NULL,
                          monthly_invest_amount BIGINT NOT NULL DEFAULT 0,
                          cash_balance BIGINT NOT NULL,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          PRIMARY KEY (account_id),

                          FOREIGN KEY (user_id)
                              REFERENCES users(user_id),

                          CHECK (
                              seed_money BETWEEN 0 AND 1000000000
                              ),

                          CHECK (
                              monthly_invest_amount
                                  BETWEEN 0 AND 10000000
                              ),

                          CHECK (
                              cash_balance >= 0
                              )
);


-- =========================================================
-- 8. 증권
-- 주식 / 주식 ETF / 채권 ETF
-- PK: security_id → 테이블명 securities
-- =========================================================

CREATE TABLE securities (
                            security_id BIGINT NOT NULL AUTO_INCREMENT,
                            ticker VARCHAR(20) NOT NULL,
                            name VARCHAR(100) NOT NULL,
                            security_type VARCHAR(20) NOT NULL,
                            sector VARCHAR(50) NULL,
                            market_cap BIGINT NULL,
                            volatility DECIMAL(6,4) NULL,
                            mdd DECIMAL(6,4) NULL,
                            average_daily_move DECIMAL(6,4) NULL,
                            average_volume BIGINT NULL,
                            created_at DATETIME NULL,
                            updated_at DATETIME NULL,

                            PRIMARY KEY (security_id),

                            CHECK (
                                security_type IN (
                                                  'STOCK',
                                                  'EQUITY_ETF',
                                                  'BOND_ETF'
                                    )
                                ),

                            CHECK (
                                volatility IS NULL
                                    OR volatility >= 0
                                ),

                            CHECK (
                                mdd IS NULL
                                    OR mdd <= 0
                                ),

                            CHECK (
                                average_daily_move IS NULL
                                    OR average_daily_move >= 0
                                )
);


-- =========================================================
-- 9. 증권 일별 가격
-- =========================================================

CREATE TABLE security_daily_prices (
                                       security_daily_prices_id BIGINT NOT NULL AUTO_INCREMENT,
                                       security_id BIGINT NOT NULL,
                                       trade_date DATE NOT NULL,
                                       open_price DECIMAL(15,2) NOT NULL,
                                       close_price DECIMAL(15,2) NOT NULL,
                                       high_price DECIMAL(15,2) NOT NULL,
                                       low_price DECIMAL(15,2) NOT NULL,
                                       volume BIGINT NOT NULL,

                                       PRIMARY KEY (security_daily_prices_id),

                                       UNIQUE (
                                               security_id,
                                               trade_date
                                           ),

                                       FOREIGN KEY (security_id)
                                           REFERENCES securities(security_id),

                                       CHECK (
                                           high_price >= low_price
                                           )
);


-- =========================================================
-- 10. 보유 증권
-- PK: holding_security_id → 테이블명 holding_securities
-- =========================================================

CREATE TABLE holding_securities (
                                    holding_security_id BIGINT NOT NULL AUTO_INCREMENT,
                                    account_id BIGINT NOT NULL,
                                    security_id BIGINT NOT NULL,
                                    quantity INT NOT NULL,
                                    average_price BIGINT NOT NULL,
                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    PRIMARY KEY (holding_security_id),

                                    FOREIGN KEY (account_id)
                                        REFERENCES accounts(account_id),

                                    FOREIGN KEY (security_id)
                                        REFERENCES securities(security_id),

                                    CHECK (
                                        quantity > 0
                                        )
);


-- =========================================================
-- 11. 금융상품
-- PK: product_id → 테이블명 products
--
-- 금감원 API 필드명은 그대로 유지
-- =========================================================

CREATE TABLE products (
                          product_id BIGINT NOT NULL AUTO_INCREMENT,
                          product_type VARCHAR(20) NOT NULL,
                          fin_co_no VARCHAR(20) NOT NULL,
                          fin_prdt_cd VARCHAR(50) NOT NULL,
                          kor_co_nm VARCHAR(100) NOT NULL,
                          fin_prdt_nm VARCHAR(200) NOT NULL,
                          join_way VARCHAR(200) NULL,
                          mtrt_int VARCHAR(1000) NULL,
                          spcl_cnd VARCHAR(2000) NULL,
                          join_member VARCHAR(500) NULL,
                          etc_note VARCHAR(2000) NULL,
                          max_limit BIGINT NULL,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          PRIMARY KEY (product_id),

                          INDEX (product_type),
                          INDEX (fin_co_no),
                          INDEX (fin_prdt_nm),

                          UNIQUE (
                                  product_type,
                                  fin_co_no,
                                  fin_prdt_cd
                              ),

                          CHECK (
                              product_type IN (
                                               'DEPOSIT',
                                               'SAVING'
                                  )
                              )
);


-- =========================================================
-- 12. 금융상품 옵션
-- PK: product_option_id → 테이블명 product_options
--
-- 금감원 API 필드명은 그대로 유지
-- =========================================================

CREATE TABLE product_options (
                                 product_option_id BIGINT NOT NULL AUTO_INCREMENT,
                                 product_id BIGINT NOT NULL,
                                 intr_rate_type VARCHAR(20) NOT NULL,
                                 intr_rate_type_nm VARCHAR(100) NOT NULL,
                                 rsrv_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
                                 rsrv_type_nm VARCHAR(100) NULL,
                                 save_trm INT NOT NULL,
                                 intr_rate DECIMAL(5,2) NULL,
                                 intr_rate2 DECIMAL(5,2) NULL,
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 PRIMARY KEY (product_option_id),

                                 INDEX (product_id),

                                 FOREIGN KEY (product_id)
                                     REFERENCES products(product_id)
                                     ON DELETE CASCADE,

                                 UNIQUE (
                                         product_id,
                                         intr_rate_type,
                                         rsrv_type,
                                         save_trm
                                     ),

                                 CHECK (
                                     rsrv_type IN (
                                                   'NONE',
                                                   'F',
                                                   'S'
                                         )
                                     )
);


-- =========================================================
-- 13. 보유 예적금
-- PK: holding_product_id → 테이블명 holding_products
-- =========================================================

CREATE TABLE holding_products (
                                  holding_product_id BIGINT NOT NULL AUTO_INCREMENT,
                                  account_id BIGINT NOT NULL,
                                  product_option_id BIGINT NOT NULL,
                                  join_amount BIGINT NOT NULL,
                                  applied_rate DECIMAL(5,2) NOT NULL,
                                  total_installments INT NULL,
                                  paid_installments INT NULL DEFAULT 0,
                                  payment_date DATE NULL,
                                  start_date DATE NOT NULL,
                                  maturity_date DATE NOT NULL,
                                  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME NOT NULL
                                      DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,

                                  PRIMARY KEY (holding_product_id),

                                  FOREIGN KEY (account_id)
                                      REFERENCES accounts(account_id),

                                  FOREIGN KEY (product_option_id)
                                      REFERENCES product_options(product_option_id),

                                  CHECK (
                                      join_amount > 0
                                      ),

                                  CHECK (
                                      applied_rate >= 0
                                      ),

                                  CHECK (
                                      total_installments IS NULL
                                          OR total_installments > 0
                                      ),

                                  CHECK (
                                      paid_installments IS NULL
                                          OR paid_installments >= 0
                                      ),

                                  CHECK (
                                      total_installments IS NULL
                                          OR paid_installments IS NULL
                                          OR paid_installments <= total_installments
                                      ),

                                  CHECK (
                                      maturity_date > start_date
                                      ),

                                  CHECK (
                                      status IN (
                                                 'ACTIVE',
                                                 'MATURED',
                                                 'TERMINATED'
                                          )
                                      )
);


-- =========================================================
-- 14. 증권 거래
-- PK: security_order_id → 테이블명 security_orders
-- =========================================================

CREATE TABLE security_orders (
                                 security_order_id BIGINT NOT NULL AUTO_INCREMENT,
                                 account_id BIGINT NOT NULL,
                                 security_id BIGINT NULL,
                                 order_type ENUM(
        'BUY',
        'SELL'
    ) NOT NULL,
                                 order_method ENUM(
        'MARKET',
        'LIMIT'
    ) NULL DEFAULT 'MARKET',
                                 order_price BIGINT NOT NULL,
                                 executed_price BIGINT NULL,
                                 quantity INT NOT NULL,
                                 status ENUM(
        'PENDING',
        'COMPLETED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',
                                 ordered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 executed_at DATETIME NULL DEFAULT NULL,
                                 updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,

                                 PRIMARY KEY (security_order_id),

                                 FOREIGN KEY (account_id)
                                     REFERENCES accounts(account_id),

                                 FOREIGN KEY (security_id)
                                     REFERENCES securities(security_id),

                                 CHECK (
                                     order_price > 0
                                     ),

                                 CHECK (
                                     quantity > 0
                                     )
);


-- =========================================================
-- 15. 예적금 거래
-- PK: product_transaction_id → 테이블명 product_transactions
-- =========================================================

CREATE TABLE product_transactions (
                                      product_transaction_id BIGINT NOT NULL AUTO_INCREMENT,
                                      holding_product_id BIGINT NOT NULL,
                                      type VARCHAR(30) NOT NULL,
                                      amount BIGINT NOT NULL,
                                      installment_number INT NULL,
                                      status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
                                      terminated_at DATETIME NULL,
                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at DATETIME NOT NULL
                                          DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,

                                      PRIMARY KEY (product_transaction_id),

                                      INDEX (holding_product_id),
                                      INDEX (type),

                                      FOREIGN KEY (holding_product_id)
                                          REFERENCES holding_products(holding_product_id),

                                      CHECK (
                                          type IN (
                                                   'SUBSCRIBE',
                                                   'PAYMENT',
                                                   'ADDITIONAL_PAYMENT',
                                                   'TERMINATE',
                                                   'MATURITY'
                                              )
                                          ),

                                      CHECK (
                                          amount >= 0
                                          ),

                                      CHECK (
                                          installment_number IS NULL
                                              OR installment_number > 0
                                          ),

                                      CHECK (
                                          status IN (
                                                     'PENDING',
                                                     'COMPLETED',
                                                     'CANCELLED'
                                              )
                                          )
);


-- =========================================================
-- 16. 계좌 거래
-- account_trasaction_id → account_transaction_id 오타 수정
-- PK: account_transaction_id → account_transactions
-- =========================================================

CREATE TABLE account_transactions (
                                      account_transaction_id BIGINT NOT NULL AUTO_INCREMENT,
                                      account_id BIGINT NOT NULL,
                                      type VARCHAR(20) NOT NULL,
                                      amount BIGINT NOT NULL,
                                      created_at DATETIME NOT NULL,
                                      updated_at DATETIME NOT NULL,

                                      PRIMARY KEY (account_transaction_id),

                                      FOREIGN KEY (account_id)
                                          REFERENCES accounts(account_id),

                                      CHECK (
                                          type IN (
                                                   'IN',
                                                   'OUT'
                                              )
                                          )
);