package org.kkobi.users.dto.request;

import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
public class FriendRequestDto {

    @NotBlank(message = "닉네임을 입력해 주세요.")
    private String nickname;
}
