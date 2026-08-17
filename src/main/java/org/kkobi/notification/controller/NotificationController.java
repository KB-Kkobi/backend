package org.kkobi.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.notification.dto.request.NotificationSettingsUpdateRequestDto;
import org.kkobi.notification.dto.response.NotificationResponseDto;
import org.kkobi.notification.dto.response.NotificationSettingsResponseDto;
import org.kkobi.notification.dto.response.NotificationUnreadCountResponseDto;
import org.kkobi.notification.service.NotificationService;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
@Tag(name = "알림", description = "인앱 알림 및 알림 설정 API")
public class NotificationController {

    private final NotificationService notificationService;

    // 로그인 사용자의 알림 설정을 조회
    @Operation(
            summary = "알림 설정 조회",
            description = "로그인 사용자의 거래 알림 및 친구 알림 수신 설정을 조회합니다."
    )
    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingsResponseDto> getSettings(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser

    ){
        NotificationSettingsResponseDto response =
                notificationService.getSettings(authenticatedUser.getUserId());

        return ResponseEntity.ok(response);
    }

    // 로그인 사용자의 알림 설정을 변경
    @Operation(
            summary = "알림 설정 변경",
            description = "거래 알림 또는 친구 알림 수신 여부를 변경합니다."
    )
    @PatchMapping("/settings")
    public ResponseEntity<NotificationSettingsResponseDto> updateSettings(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @RequestBody NotificationSettingsUpdateRequestDto request
    ){
        Long userId = authenticatedUser.getUserId();

        notificationService.updateSettings(
                userId,
                request
        );

        NotificationSettingsResponseDto response =
                notificationService.getSettings(userId);

        return ResponseEntity.ok(response);
    }

    // 로그인 사용자의 알림 목록을 조회
    @Operation(
            summary = "알림 목록 조회",
            description = "로그인 사용자의 인앱 알림 목록을 최신순으로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        List<NotificationResponseDto> response =
                notificationService.getNotifications(
                        authenticatedUser.getUserId()
                );

        return ResponseEntity.ok(response);
    }

    // 읽지 않은 알림 개수를 조회
    @Operation(
            summary = "읽지 않은 알림 개수 조회",
            description = "로그인 사용자의 읽지 않은 알림 개수를 조회합니다."
    )
    @GetMapping("/unread-count")
    public ResponseEntity<NotificationUnreadCountResponseDto> getUnreadCount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ){
        int unreadCount =
                notificationService.getUnreadCount(
                        authenticatedUser.getUserId()
                );

        return ResponseEntity.ok(
                new NotificationUnreadCountResponseDto(unreadCount)
        );
    }

    // 개별 알림을 읽음 처리
    @Operation(
            summary = "개별 알림 읽음 처리",
            description = "로그인 사용자의 특정 알림을 읽음 처리합니다."
    )
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(
                authenticatedUser.getUserId(),
                notificationId
        );

        return ResponseEntity.noContent().build();
    }

    // 모든 알림을 읽음 처리
    @Operation(
            summary = "전체 알림 읽음 처리",
            description = "로그인 사용자의 읽지 않은 모든 알림을 읽음 처리합니다."
    )
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ){
        notificationService.markAllAsRead(
                authenticatedUser.getUserId()
        );

        return ResponseEntity.noContent().build();
    }
}
