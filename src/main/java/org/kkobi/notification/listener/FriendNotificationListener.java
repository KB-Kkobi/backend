package org.kkobi.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkobi.notification.enums.NotificationType;
import org.kkobi.notification.service.NotificationService;
import org.kkobi.users.event.FriendRequestAcceptedEvent;
import org.kkobi.users.event.FriendRequestCancelledEvent;
import org.kkobi.users.event.FriendRequestSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendNotificationListener {

    private final NotificationService notificationService;

    // 친구 신청 트랜잭션이 정상 커밋된 후 수신자에게 알림 생성
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void onFriendRequestSent(FriendRequestSentEvent event){

        try{
            notificationService.createNotification(
                    event.receiverId(),
                    NotificationType.FRIEND_REQUEST_RECEIVED,
                    "친구 신청",
                    event.requesterNickname() +"님이 친구 신청을 보냈어요",
                    event.friendshipId()
            );
        } catch (Exception e){
            log.warn(
                    "친구 신청 알림 생성 실패 friendshipId={} receiverId={} error={}",
                    event.friendshipId(),
                    event.receiverId(),
                    e.getMessage()
            );
        }
    }

    // 친구 요청 수락 트랜잭션이 정상 커밋된 후 신청자에게 알림 생성
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void onFriendRequestAccepted(
            FriendRequestAcceptedEvent event
    ){

        try{
            notificationService.createNotification(
                    event.requesterId(),
                    NotificationType.FRIEND_REQUEST_RECEIVED,
                    "친구 신청 수락",
                    event.receiverNickname() + "님이 친구 신청을 수락했어요",
                    event.friendshipId()
            );
        } catch (Exception e){
            log.warn(
                    "친구 수락 알림 생성 실패 friendshipId={} requesterId={} error={}",
                    event.friendshipId(),
                    event.requesterId(),
                    e.getMessage()
            );
        }
    }

    // 친구 요청 취소 후 기존 친구 신청 알림 삭제
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFriendRequestCancelled(FriendRequestCancelledEvent event){

        try{
            notificationService.deleteNotificationByReference(
                    event.receiverId(),
                    NotificationType.FRIEND_REQUEST_RECEIVED,
                    event.friendshipId()
            );
        } catch (Exception e){
            log.warn(
                    "친구 요청 취소 알림 삭제 실패. friendshipId={}, receiverId={}",
                    event.friendshipId(),
                    event.receiverId(),
                    e
            );
        }
    }
}
