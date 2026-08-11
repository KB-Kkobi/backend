package org.kkobi.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kkobi.security.jwt.JwtToken;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private long accessTokenExpiresAt;
    private long refreshTokenExpiresAt;

    public static TokenResponse from(JwtToken token) {
        return new TokenResponse(
                token.getAccessToken(),
                token.getTokenType(),
                token.getAccessTokenExpiresAt(),
                token.getRefreshTokenExpiresAt()
        );
    }
}
