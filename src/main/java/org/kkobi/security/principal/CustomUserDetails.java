package org.kkobi.security.principal;

import org.kkobi.users.domain.UserVO;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

// Spring Security 인증정보에 회원 ID를 포함하기 위한 사용자 객체
public class CustomUserDetails extends User {

    private final Long userId;

    public CustomUserDetails(UserVO user){
        super(
                user.getEmail(),
                user.getPassword(),
                Collections.emptyList()
        );

        this.userId = user.getUserId();
    }

    public Long getUserId() {
        return userId;
    }
}
