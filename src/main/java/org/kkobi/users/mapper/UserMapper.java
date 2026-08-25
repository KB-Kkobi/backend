package org.kkobi.users.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.users.domain.UserVO;
import org.kkobi.users.enums.ProfileImageType;

import java.time.LocalDate;

public interface UserMapper {

    // 신규 사용자를 저장하고 생성된 userId를 UserVO에 채움
    int insert(UserVO user);

    // userId로 사용자를 조회
    UserVO findById(@Param("userId") Long userId);

    // 이메일로 사용자를 조회
    UserVO findByEmail(@Param("email") String email);

    // 닉네임으로 사용자를 조회
    UserVO findByNickname(@Param("nickname") String nickname);

    // 이메일 중복 여부 확인을 위해 개수를 조회
    int countByEmail(@Param("email") String email);

    // 닉네임 중복 여부 확인을 위해 개수를 조회
    int countByNickname(@Param("nickname") String nickname);

    // 사용자 기본 정보를 수정
    int update(UserVO user);

    int updateProfile(@Param("userId") Long userId,
                      @Param("nickname") String nickname,
                      @Param("birthDate") LocalDate birthDate,
                      @Param("profileImage") ProfileImageType profileImage);

    // 사용자 비밀번호를 수정
    int updatePassword(UserVO user);

    // userId로 사용자를 삭제
    int deleteById(@Param("userId") Long userId);
}
