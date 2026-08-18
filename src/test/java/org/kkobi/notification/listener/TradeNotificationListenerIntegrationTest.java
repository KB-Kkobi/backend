package org.kkobi.notification.listener;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.notification.dto.request.NotificationSettingsUpdateRequestDto;
import org.kkobi.notification.dto.response.NotificationResponseDto;
import org.kkobi.notification.enums.NotificationType;
import org.kkobi.notification.service.NotificationService;
import org.kkobi.notification.service.NotificationServiceImpl;
import org.kkobi.trade.enums.OrderType;
import org.kkobi.trade.event.TradeOrderFilledEvent;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = TradeNotificationListenerIntegrationTest.TradeNotificationTestConfig.class
)
class TradeNotificationListenerIntegrationTest {

    // 현재 로컬 테스트 데이터
    private static final Long USER_ID = 28L;
    private static final Long ACCOUNT_ID = 3L;
    private static final Long SECURITY_ID = 1L;

    // 실제 주문 ID와 겹치지 않도록 테스트 전용 referenceId 사용
    private static final Long BUY_ORDER_ID = 990001L;
    private static final Long SELL_ORDER_ID = 990002L;
    private static final Long DISABLED_ORDER_ID = 990003L;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private JdbcTemplate jdbcTemplate;

    private boolean settingsExisted;
    private boolean originalTradeEnabled;
    private boolean originalFriendEnabled;


    @Autowired
    void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    @BeforeEach
    void setUp() {

        // 테스트 전 기존 테스트 알림 제거
        deleteTestNotifications();

        // 기존 알림 설정 백업
        List<Map<String, Object>> settings =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            trade_enabled,
                            friend_enabled
                        FROM notification_settings
                        WHERE user_id = ?
                        """,
                        USER_ID
                );

        settingsExisted = !settings.isEmpty();

        if (settingsExisted) {

            Map<String, Object> row =
                    settings.get(0);

            originalTradeEnabled =
                    toBoolean(row.get("trade_enabled"));

            originalFriendEnabled =
                    toBoolean(row.get("friend_enabled"));
        }
    }


    @AfterEach
    void tearDown() {

        // 테스트에서 생성한 알림 제거
        deleteTestNotifications();

        // 테스트 전 알림 설정으로 복구
        if (settingsExisted) {

            jdbcTemplate.update(
                    """
                    UPDATE notification_settings
                    SET
                        trade_enabled = ?,
                        friend_enabled = ?
                    WHERE user_id = ?
                    """,
                    originalTradeEnabled,
                    originalFriendEnabled,
                    USER_ID
            );

        } else {

            jdbcTemplate.update(
                    """
                    DELETE FROM notification_settings
                    WHERE user_id = ?
                    """,
                    USER_ID
            );
        }
    }


    // 매수 체결 이벤트 → 매수 알림 생성
    @Test
    @DisplayName("매수 체결 후 매수 알림이 생성된다")
    void buyFilled_createsNotification() {

        enableTradeNotification();

        TradeOrderFilledEvent event =
                new TradeOrderFilledEvent(
                        ACCOUNT_ID,
                        BUY_ORDER_ID,
                        SECURITY_ID,
                        OrderType.BUY,
                        1,
                        72000L
                );

        publishAfterCommit(event);

        NotificationResponseDto notification =
                findNotification(BUY_ORDER_ID);

        assertNotNull(notification);

        assertEquals(
                NotificationType.TRADE_BUY_FILLED,
                notification.getType()
        );

        assertEquals(
                "매수 체결",
                notification.getTitle()
        );

        assertEquals(
                "삼성전자 1주 매수가 72,000원에 체결됐어요.",
                notification.getMessage()
        );

        assertEquals(
                BUY_ORDER_ID,
                notification.getReferenceId()
        );

        assertFalse(notification.getRead());
        assertNull(notification.getReadAt());
    }


    // 매도 체결 이벤트 → 매도 알림 생성
    @Test
    @DisplayName("매도 체결 후 매도 알림이 생성된다")
    void sellFilled_createsNotification() {

        enableTradeNotification();

        TradeOrderFilledEvent event =
                new TradeOrderFilledEvent(
                        ACCOUNT_ID,
                        SELL_ORDER_ID,
                        SECURITY_ID,
                        OrderType.SELL,
                        2,
                        73500L
                );

        publishAfterCommit(event);

        NotificationResponseDto notification =
                findNotification(SELL_ORDER_ID);

        assertNotNull(notification);

        assertEquals(
                NotificationType.TRADE_SELL_FILLED,
                notification.getType()
        );

        assertEquals(
                "매도 체결",
                notification.getTitle()
        );

        assertEquals(
                "삼성전자 2주 매도가 73,500원에 체결됐어요.",
                notification.getMessage()
        );

        assertEquals(
                SELL_ORDER_ID,
                notification.getReferenceId()
        );

        assertFalse(notification.getRead());
    }


    // 거래 알림 OFF → 체결 이벤트가 발생해도 알림 저장 X
    @Test
    @DisplayName("거래 알림이 비활성화되면 체결 알림을 저장하지 않는다")
    void tradeNotificationDisabled_doesNotCreateNotification() {

        NotificationSettingsUpdateRequestDto request =
                new NotificationSettingsUpdateRequestDto();

        request.setTradeEnabled(false);

        notificationService.updateSettings(
                USER_ID,
                request
        );

        TradeOrderFilledEvent event =
                new TradeOrderFilledEvent(
                        ACCOUNT_ID,
                        DISABLED_ORDER_ID,
                        SECURITY_ID,
                        OrderType.BUY,
                        1,
                        72000L
                );

        publishAfterCommit(event);

        NotificationResponseDto notification =
                findNotification(DISABLED_ORDER_ID);

        assertNull(notification);
    }


    // 거래 알림 ON
    private void enableTradeNotification() {

        NotificationSettingsUpdateRequestDto request =
                new NotificationSettingsUpdateRequestDto();

        request.setTradeEnabled(true);

        notificationService.updateSettings(
                USER_ID,
                request
        );
    }


    /*
     * TransactionalEventListener(AFTER_COMMIT)을 실제로 실행시키기 위해
     * TransactionTemplate 안에서 이벤트를 발행하고 정상 COMMIT 시킨다.
     */
    private void publishAfterCommit(
            TradeOrderFilledEvent event
    ) {

        transactionTemplate.executeWithoutResult(
                status -> eventPublisher.publishEvent(event)
        );
    }


    // referenceId로 테스트 알림 조회
    private NotificationResponseDto findNotification(
            Long referenceId
    ) {

        return notificationService
                .getNotifications(USER_ID)
                .stream()
                .filter(notification ->
                        referenceId.equals(
                                notification.getReferenceId()
                        )
                )
                .findFirst()
                .orElse(null);
    }


    // 테스트용 알림만 제거
    private void deleteTestNotifications() {

        jdbcTemplate.update(
                """
                DELETE FROM notifications
                WHERE user_id = ?
                  AND reference_id IN (?, ?, ?)
                """,
                USER_ID,
                BUY_ORDER_ID,
                SELL_ORDER_ID,
                DISABLED_ORDER_ID
        );
    }


    /*
     * 거래 알림 Integration Test 전용 설정
     *
     * RootConfig 전체를 띄우지 않고
     * 이번 테스트에 필요한 Bean만 로드한다.
     */
    @Configuration
    @EnableTransactionManagement
    @PropertySource("classpath:/application.properties")
    @MapperScan(basePackages = {
            "org.kkobi.notification.mapper",
            "org.kkobi.trade.mapper"
    })
    @Import({
            NotificationServiceImpl.class,
            TradeNotificationListener.class
    })
    static class TradeNotificationTestConfig {

        @Value("${jdbc.driver}")
        private String driver;

        @Value("${jdbc.url}")
        private String url;

        @Value("${jdbc.username}")
        private String username;

        @Value("${jdbc.password}")
        private String password;

        @Autowired
        private ApplicationContext applicationContext;


        @Bean
        public DataSource dataSource() {

            HikariConfig config =
                    new HikariConfig();

            config.setDriverClassName(driver);
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);

            return new HikariDataSource(config);
        }


        @Bean
        public SqlSessionFactory sqlSessionFactory()
                throws Exception {

            SqlSessionFactoryBean factoryBean =
                    new SqlSessionFactoryBean();

            factoryBean.setConfigLocation(
                    applicationContext.getResource(
                            "classpath:/mybatis-config.xml"
                    )
            );

            factoryBean.setDataSource(
                    dataSource()
            );

            return factoryBean.getObject();
        }


        @Bean
        public DataSourceTransactionManager transactionManager() {

            return new DataSourceTransactionManager(
                    dataSource()
            );
        }


        @Bean
        public TransactionTemplate transactionTemplate(
                DataSourceTransactionManager transactionManager
        ) {

            return new TransactionTemplate(
                    transactionManager
            );
        }
    }

    private boolean toBoolean(Object value) {

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }

        return Boolean.parseBoolean(
                String.valueOf(value)
        );
    }
}