package org.kkobi.users.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRequestResponseDto {

    // 친구 요청을 식별하는 ID
    private Long friendshipId;

    // 친구 요청을 보낸 사용자의 ID
    private Long requesterId;

    // 친구 요청을 보낸 사용자의 닉네임
    private String nickname;

    // 친구 요청을 보낸 시간
    private LocalDateTime createdAt;
}
