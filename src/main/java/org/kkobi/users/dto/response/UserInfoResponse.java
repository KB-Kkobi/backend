package org.kkobi.users.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kkobi.users.domain.UserVO;
import org.kkobi.users.enums.ProfileImageType;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long userId;
    private String email;
    private String nickname;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate birthDate;
    private ProfileImageType profileImage;

    public static UserInfoResponse from(UserVO user) {
        return new UserInfoResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getBirthDate(),
                user.getProfileImage() == null
                        ? ProfileImageType.SLEEP_KKOBI
                        : user.getProfileImage()
        );
    }
}
