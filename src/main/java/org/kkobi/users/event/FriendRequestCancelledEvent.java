package org.kkobi.users.event;

/**
 * 보낸 친구 요청이 취소되었을 때 발생하는 이벤트
 *
 * @param friendshipId 취소된 친구 관계 ID
 * @param requesterId  친구 요청을 보낸 사용자 ID
 * @param receiverId   친구 요청을 받은 사용자 ID
 */
public record FriendRequestCancelledEvent(
        Long friendshipId,
        Long requesterId,
        Long receiverId
) {
}
