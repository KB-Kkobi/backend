package org.kkobi.users.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.security.principal.CustomUserDetails;
import org.kkobi.users.domain.UserVO;
import org.kkobi.users.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// DB에서 사용자 정보를 조회해 Spring Security 인증 정보로 변환하는 구현체
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    // 이메일로 사용자 정보를 조회하는 Mapper
    private final UserMapper userMapper;

    // Spring Security 로그인 인증에 사용할 사용자를 이메일로 조회
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserVO user = userMapper.findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return new CustomUserDetails(user);
    }
}
