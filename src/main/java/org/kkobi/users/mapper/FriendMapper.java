package org.kkobi.users.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.users.dto.response.FriendRequestResponseDto;
import org.kkobi.users.dto.response.FriendResponseDto;
import org.kkobi.users.enums.FriendshipStatus;
import org.springframework.security.core.parameters.P;

import java.util.List;

public interface FriendMapper {

    // 두 사용자 사이에 친구 관계 또는 요청이 존재하는지 확인
    int countRelationship(
            @Param("userId") Long userId,
            @Param("targetUserId") Long targetUserId
    );

    // 친구 요청을 저장
    int insertFriendRequest(
            @Param("requesterId") Long requesterId,
            @Param("receiverId") Long receiverId,
            @Param("status") FriendshipStatus status
            );

    // 받은 친구 요청 목록을 조회
    List<FriendRequestResponseDto> findReceivedFriendRequests(
            @Param("userId") Long userId
    );

    // 받은 친구 요청을 수락
    int acceptFriendRequest(
            @Param("friendshipId") Long friendshipId,
            @Param("receiverId") Long receiverId
    );

    // 받은 친구 요청을 거절
    int rejectFriendRequest(
            @Param("friendshipId") Long friendshipId,
            @Param("receiverId") Long receiverId
    );

    // 친구 목록을 조회
    List<FriendResponseDto> findFriends(
            @Param("userId") Long userId
    );

    // 친구 관계를 삭제
    int deleteFriend(
            @Param("userId") Long userId,
            @Param("friendUserId") Long friendUserId
    );
}
