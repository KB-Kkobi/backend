package org.kkobi.notification.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.notification.dto.request.NotificationSettingsUpdateRequestDto;
import org.kkobi.notification.dto.response.NotificationResponseDto;
import org.kkobi.notification.dto.response.NotificationSettingsResponseDto;
import org.kkobi.notification.enums.NotificationType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = NotificationServiceIntegrationTest.NotificationTestConfig.class
)
@Transactional
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    // 알림 설정이 없는 기존 사용자는 모든 알림을 기본 허용
    @Test
    @DisplayName("알림 설정이 없으면 거래와 친구 알림을 기본 허용한다")
    void getSettings_returnsDefaultEnabled_whenSettingsNotExists() {

        Long userId = createUser();

        NotificationSettingsResponseDto settings =
                notificationService.getSettings(userId);

        assertTrue(settings.isTradeEnabled());
        assertTrue(settings.isFriendEnabled());
    }


    // 일부 설정만 변경하면 나머지 설정은 기존 값을 유지
    @Test
    @DisplayName("거래 알림만 변경하면 친구 알림 설정은 유지된다")
    void updateSettings_updatesOnlyRequestedSetting() {

        Long userId = createUser();

        NotificationSettingsUpdateRequestDto request =
                new NotificationSettingsUpdateRequestDto();

        request.setTradeEnabled(false);

        notificationService.updateSettings(userId, request);

        NotificationSettingsResponseDto settings =
                notificationService.getSettings(userId);

        assertFalse(settings.isTradeEnabled());
        assertTrue(settings.isFriendEnabled());
    }


    // 알림 생성 → 목록 조회 → unread 조회 → 읽음 처리 전체 흐름 검증
    @Test
    @DisplayName("알림 생성 후 목록 조회와 읽음 처리가 정상 동작한다")
    void notificationLifecycle_worksCorrectly() {

        Long userId = createUser();

        notificationService.createNotification(
                userId,
                NotificationType.TRADE_BUY_FILLED,
                "매수 체결",
                "삼성전자 3주 매수가 체결됐어요.",
                100L
        );

        List<NotificationResponseDto> notifications =
                notificationService.getNotifications(userId);

        assertEquals(1, notifications.size());

        NotificationResponseDto notification =
                notifications.get(0);

        assertNotNull(notification.getNotificationId());

        assertEquals(
                NotificationType.TRADE_BUY_FILLED,
                notification.getType()
        );

        assertEquals(
                "매수 체결",
                notification.getTitle()
        );

        assertEquals(
                "삼성전자 3주 매수가 체결됐어요.",
                notification.getMessage()
        );

        assertEquals(
                100L,
                notification.getReferenceId()
        );

        assertFalse(notification.getRead());
        assertNull(notification.getReadAt());

        assertEquals(
                1,
                notificationService.getUnreadCount(userId)
        );


        // 개별 알림 읽음 처리
        notificationService.markAsRead(
                userId,
                notification.getNotificationId()
        );


        assertEquals(
                0,
                notificationService.getUnreadCount(userId)
        );

        NotificationResponseDto readNotification =
                notificationService
                        .getNotifications(userId)
                        .get(0);

        assertTrue(readNotification.getRead());
        assertNotNull(readNotification.getReadAt());
    }


    // 거래 알림 OFF 상태에서는 거래 알림을 저장하지 않음
    @Test
    @DisplayName("거래 알림이 비활성화되면 거래 알림을 저장하지 않는다")
    void createNotification_doesNotSaveTradeNotification_whenDisabled() {

        Long userId = createUser();

        NotificationSettingsUpdateRequestDto request =
                new NotificationSettingsUpdateRequestDto();

        request.setTradeEnabled(false);

        notificationService.updateSettings(userId, request);


        // 거래 알림 생성 시도
        notificationService.createNotification(
                userId,
                NotificationType.TRADE_SELL_FILLED,
                "매도 체결",
                "삼성전자 2주 매도가 체결됐어요.",
                101L
        );


        // 거래 알림 OFF이므로 저장되지 않아야 함
        assertEquals(
                0,
                notificationService.getNotifications(userId).size()
        );


        // 친구 알림은 기본 ON이므로 저장되어야 함
        notificationService.createNotification(
                userId,
                NotificationType.FRIEND_REQUEST_RECEIVED,
                "친구 신청",
                "테스트사용자님이 친구 신청을 보냈어요.",
                1L
        );

        assertEquals(
                1,
                notificationService.getNotifications(userId).size()
        );
    }


    // 여러 알림 전체 읽음 처리
    @Test
    @DisplayName("모든 알림을 한 번에 읽음 처리한다")
    void markAllAsRead_marksEveryNotificationAsRead() {

        Long userId = createUser();

        notificationService.createNotification(
                userId,
                NotificationType.TRADE_BUY_FILLED,
                "매수 체결",
                "삼성전자 매수가 체결됐어요.",
                1L
        );

        notificationService.createNotification(
                userId,
                NotificationType.FRIEND_REQUEST_RECEIVED,
                "친구 신청",
                "친구 신청이 도착했어요.",
                2L
        );


        assertEquals(
                2,
                notificationService.getUnreadCount(userId)
        );


        notificationService.markAllAsRead(userId);


        assertEquals(
                0,
                notificationService.getUnreadCount(userId)
        );


        List<NotificationResponseDto> notifications =
                notificationService.getNotifications(userId);

        assertEquals(2, notifications.size());

        assertTrue(
                notifications.stream()
                        .allMatch(notification ->
                                Boolean.TRUE.equals(notification.getRead())
                        )
        );

        assertTrue(
                notifications.stream()
                        .allMatch(notification ->
                                notification.getReadAt() != null
                        )
        );
    }


    // 테스트용 사용자 생성
    private Long createUser() {

        String identifier =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8);

        String email =
                "notification-" + identifier + "@test.com";


        jdbcTemplate.update(
                "INSERT INTO users " +
                        "(email, password, nickname, birth_date, postal_code, address_line1) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                email,
                "password",
                "notification-" + identifier,
                "2000-01-01",
                "00000",
                "테스트 주소"
        );


        return jdbcTemplate.queryForObject(
                "SELECT user_id " +
                        "FROM users " +
                        "WHERE email = ?",
                Long.class,
                email
        );
    }


    /*
     * 알림 Integration Test 전용 Spring 설정
     *
     * RootConfig 전체를 사용하지 않고
     * 알림 테스트에 필요한 구성요소만 로드한다.
     *
     * - DataSource
     * - TransactionManager
     * - MyBatis
     * - NotificationMapper
     * - NotificationServiceImpl
     */
    @Configuration
    @EnableTransactionManagement
    @PropertySource("classpath:/application.properties")
    @MapperScan("org.kkobi.notification.mapper")
    @Import(NotificationServiceImpl.class)
    static class NotificationTestConfig {

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


        // 테스트용 DB Connection Pool
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


        // MyBatis SqlSessionFactory
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


        // 테스트 트랜잭션 관리자
        @Bean
        public DataSourceTransactionManager transactionManager() {

            return new DataSourceTransactionManager(
                    dataSource()
            );
        }
    }
}