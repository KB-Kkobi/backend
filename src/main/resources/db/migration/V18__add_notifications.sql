-- 사용자별 알림 수신 설정을 저장
CREATE TABLE notification_settings (
                                       user_id BIGINT NOT NULL,
                                       trade_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                       friend_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                           ON UPDATE CURRENT_TIMESTAMP,

                                       PRIMARY KEY (user_id),

                                       CONSTRAINT fk_notification_settings_user
                                           FOREIGN KEY (user_id)
                                               REFERENCES users(user_id)
                                               ON DELETE CASCADE
);


-- 사용자에게 발생한 인앱 알림을 저장
CREATE TABLE notifications (
                               notification_id BIGINT NOT NULL AUTO_INCREMENT,
                               user_id BIGINT NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               title VARCHAR(100) NOT NULL,
                               message VARCHAR(255) NOT NULL,
                               reference_id BIGINT NULL,
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               read_at DATETIME NULL,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               PRIMARY KEY (notification_id),

                               CONSTRAINT fk_notifications_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(user_id)
                                       ON DELETE CASCADE,

                               INDEX idx_notifications_user_created_at
                                   (user_id, created_at DESC),

                               INDEX idx_notifications_user_is_read
                                   (user_id, is_read)
);