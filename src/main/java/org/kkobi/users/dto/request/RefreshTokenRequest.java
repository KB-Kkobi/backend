package org.kkobi.users.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh Token은 필수 입력 값입니다.")
    private String refreshToken;
}
