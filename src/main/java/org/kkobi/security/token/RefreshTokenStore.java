package org.kkobi.security.token;

import java.time.Duration;

// 활성 Refresh Token의 해시만 저장하는 저장소
public interface RefreshTokenStore {

    void save(String tokenId, String tokenHash, Duration timeToLive);

    boolean consume(String tokenId, String tokenHash);
}
