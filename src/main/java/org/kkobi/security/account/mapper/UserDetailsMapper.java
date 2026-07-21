package org.kkobi.security.account.mapper;

import org.kkobi.security.account.domain.MemberVO;

public interface UserDetailsMapper {
    public MemberVO get(String username);
}
