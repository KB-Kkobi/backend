package org.kkobi.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.security.principal.CustomUserDetails;
import org.kkobi.users.dto.request.FriendRequestDto;
import org.kkobi.users.dto.response.FriendRequestResponseDto;
import org.kkobi.users.dto.response.MessageResponse;
import org.kkobi.users.service.FriendService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends")
@Tag(name = "친구", description = "친구 관계 관리 API")
public class FriendController {

    private final FriendService friendService;

    // 닉네임 으로 친구 요청
    @Operation(
            summary = "친구 요청",
            description = "닉네임으로 사용자를 찾아 친구 요청을 보냅니다."
    )
    @PostMapping("/requests")
    public ResponseEntity<MessageResponse> sendFriendRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @Valid @RequestBody FriendRequestDto request
            ) {
        friendService.sendFriendRequest(
                authenticatedUser.getUserId(),
                request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse("친구 요청을 보냈습니다."));
    }

    // 받은 친구 요청 목록을 조회
    @Operation(
            summary = "받은 친구 요청 조회",
            description = "로그인한 사용자가 받은 대기 중인 친구 요청 목록을 조회합니다"
    )
    @GetMapping("/requests/received")
    public ResponseEntity<List<FriendRequestResponseDto>> getReceivedFriendRequests(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        List<FriendRequestResponseDto> requests =
                friendService.getReceiverFriendRequests(authenticatedUser.getUserId());

        return ResponseEntity.ok(requests);
    }

    // 받은 친구 요청을 수락
    @Operation(
            summary = "친구 요청 수락",
            description = "로그인한 사용자가 받은 대기 중인 친구 요청을 수락합니다."
    )
    @PatchMapping("/requests/{friendshipId}/accept")
    public ResponseEntity<MessageResponse> acceptFriendRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticateUser,
            @PathVariable Long friendshipId
    ) {
        friendService.acceptFriendRequest(
                authenticateUser.getUserId(),
                friendshipId
        );

        return ResponseEntity.ok(
                new MessageResponse("친구 요청을 수락했습니다.")
        );
    }

    // 받은 친구 요청을 거절
    @Operation(
            summary = "친구 요청 거절",
            description = "로그인한 사용자가 받은 대기 중인 친구 요청을 거절합니다."
    )
    @PatchMapping("/requests/{friendshipId}/reject")
    public ResponseEntity<MessageResponse> rejectFriendRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @PathVariable("friendshipId") Long friendshipId
    ) {
        friendService.rejectFriendRequest(
                authenticatedUser.getUserId(),
                friendshipId
        );

        return ResponseEntity.ok(
                new MessageResponse("친구 요청을 거절했습니다.")
        );
    }
}
