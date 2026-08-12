package org.kkobi.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.security.principal.CustomUserDetails;
import org.kkobi.users.dto.request.FriendRequestDto;
import org.kkobi.users.dto.response.MessageResponse;
import org.kkobi.users.service.FriendService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

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
}
