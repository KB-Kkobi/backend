package org.kkobi.users.event;

public record FriendRequestSentEvent (Long friendshipId,
                                      Long requesterId,
                                      String requesterNickname,
                                      Long receiverId)
    {
    }
