package org.kkobi.users.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

// 프로젝트의 사용자 인증 조회 기능을 정의하는 서비스 인터페이스
public interface UserDetailsService
        extends org.springframework.security.core.userdetails.UserDetailsService {

    // 이메일을 기준으로 Spring Security 인증 정보를 조회
    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
