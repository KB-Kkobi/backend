package org.kkobi.account.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.account.dto.AccountCreateRequestDto;

public interface AccountMapper {

    // 해당 사용자의 계좌가 이미 존재하는지 확인
    boolean existsAccountByUserId(
            @Param("userId") Long userId);

    // 초기 투자금과 월 투자금을 반영하여 계좌 생성
    int saveAccount(
            @Param("userId") Long userId,
            @Param("request")AccountCreateRequestDto request
            );
}
