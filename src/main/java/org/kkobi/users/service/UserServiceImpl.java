package org.kkobi.users.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.exception.DuplicateUserException;
import org.kkobi.users.domain.UserVO;
import org.kkobi.users.dto.request.SignupRequest;
import org.kkobi.users.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

// 회원가입 비즈니스 로직을 처리하는 서비스 구현체
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // 사용자 정보 저장 및 중복 조회에 사용하는 Mapper
    private final UserMapper userMapper;
    // 비밀번호를 안전하게 암호화하는 인코더
    private final PasswordEncoder passwordEncoder;

    // 회원가입 정보를 검증하고 암호화한 뒤 DB에 저장
    @Override
    @Transactional
    public Long signup(SignupRequest request) {
        validatePasswordDoesNotContainEmail(request.getPassword(), request.getEmail());
        validateEmailNotDuplicated(request.getEmail());
        validateNicknameNotDuplicated(request.getNickname());

        UserVO user = toUserVO(request);
        int insertedRows = userMapper.insert(user);

        if (insertedRows != 1 || user.getUserId() == null) {
            throw new IllegalStateException("회원가입 처리 중 사용자 저장에 실패했습니다.");
        }

        return user.getUserId();
    }

    // 비밀번호에 로그인 아이디인 이메일이 포함됐는지 검사
    private void validatePasswordDoesNotContainEmail(String password, String email) {
        if (password == null || email == null) {
            return;
        }

        if (password.toLowerCase(Locale.ROOT).contains(email.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("비밀번호에는 이메일을 포함할 수 없습니다.");
        }

    }

    // 이미 가입된 이메일인지 검사
    private void validateEmailNotDuplicated(String email) {
        if (userMapper.countByEmail(email) > 0) {
            throw new DuplicateUserException("이미 사용 중인 이메일입니다.");
        }
    }

    // 이미 사용 중인 닉네임인지 검사
    private void validateNicknameNotDuplicated(String nickname) {
        if (userMapper.countByNickname(nickname) > 0) {
            throw new DuplicateUserException("이미 사용 중인 닉네임입니다.");
        }
    }

    // 회원가입 DTO를 DB 저장용 VO로 변환하고 비밀번호를 암호화
    private UserVO toUserVO(SignupRequest request) {
        UserVO user = new UserVO();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setBirthDate(request.getBirthDate());
        user.setPostalCode(request.getPostalCode());
        user.setAddressLine1(request.getAddressLine1());
        user.setAddressLine2(request.getAddressLine2());
        return user;
    }
}
