package org.kkobi.users.event;

public record FriendRequestAcceptedEvent (Long friendshipId,
                                          Long requesterId,
                                          Long receiverId,
                                          String receiverNickname)
{
}
