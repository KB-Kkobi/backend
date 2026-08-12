package org.kkobi.users.service;

import org.kkobi.users.dto.request.FriendRequestDto;

public interface FriendService {

    // 닉네임으로 친구 요청을 전송
    void sendFriendRequest(Long userId, FriendRequestDto request);
}
