package org.kkobi.users.service;

import org.kkobi.users.dto.request.SignupRequest;
import org.kkobi.users.dto.response.UserInfoResponse;

// 사용자 회원가입 기능을 정의하는 서비스 인터페이스
public interface UserService {

    // 회원가입을 처리하고 생성된 사용자 ID를 반환
    Long signup(SignupRequest request);

    // 사용자 ID로 기본 정보를 조회
    UserInfoResponse getUserInfo(Long userId);
}
