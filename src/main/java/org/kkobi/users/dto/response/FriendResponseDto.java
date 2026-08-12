package org.kkobi.users.dto.response;

import lombok.Data;

@Data
public class FriendResponseDto {
    
    // 친구 사용자의 ID
    private Long userId;
    
    // 친구 사용자의 닉네임
    private String nickname;
}
