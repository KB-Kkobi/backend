package org.kkobi.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kkobi.users.domain.UserVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long userId;
    private String nickname;

    public static UserInfoResponse from(UserVO user) {
        return new UserInfoResponse(user.getUserId(), user.getNickname());
    }
}