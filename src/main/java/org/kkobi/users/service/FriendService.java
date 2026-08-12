package org.kkobi.users.service;

import org.kkobi.users.dto.request.FriendRequestDto;
import org.kkobi.users.dto.response.FriendRequestResponseDto;
import org.kkobi.users.dto.response.FriendResponseDto;

import java.util.List;

public interface FriendService {

    // 닉네임으로 친구 요청을 전송
    void sendFriendRequest(Long userId, FriendRequestDto request);

    // 받은 친구 요청 목록을 조회
    List<FriendRequestResponseDto> getReceiverFriendRequests(Long userId);

    // 받은 친구 요청을 수락
    void acceptFriendRequest(Long userId, Long friendshipId);

    // 받은 친구 요청을 거절
    void rejectFriendRequest(Long userId, Long friendshipId);

    // 친구 목록을 조회
    List<FriendResponseDto> getFriend(Long userId);
}
