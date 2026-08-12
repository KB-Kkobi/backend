package org.kkobi.users.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.users.enums.FriendshipStatus;

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
}
