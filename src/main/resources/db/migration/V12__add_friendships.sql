-- 사용자 간 친구 요청 및 친구 관계를 저장
CREATE TABLE friendships (
                             friendship_id BIGINT NOT NULL AUTO_INCREMENT,
                             requester_id BIGINT NOT NULL,
                             receiver_id BIGINT NOT NULL,
                             status VARCHAR(20) NOT NULL,
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,

                             PRIMARY KEY (friendship_id),

                             CONSTRAINT fk_friendships_requester
                                 FOREIGN KEY (requester_id)
                                     REFERENCES users(user_id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_friendships_receiver
                                 FOREIGN KEY (receiver_id)
                                     REFERENCES users(user_id)
                                     ON DELETE CASCADE,

                             CONSTRAINT uk_friendships_requester_receiver
                                 UNIQUE (requester_id, receiver_id),

                             CONSTRAINT chk_friendships_status
                                 CHECK (status IN ('PENDING', 'ACCEPTED')),

                             CONSTRAINT chk_friendships_different_users
                                 CHECK (requester_id <> receiver_id)
);