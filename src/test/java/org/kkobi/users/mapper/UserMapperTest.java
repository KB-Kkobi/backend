package org.kkobi.users.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.config.RootConfig;
import org.kkobi.security.config.SecurityConfig;
import org.kkobi.users.domain.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, SecurityConfig.class})
@Transactional
@Rollback
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    @DisplayName("사용자 CRUD 쿼리가 정상 동작한다.")
    void userCrud() {
        UserVO user = createUser();

        int insertCount = userMapper.insert(user);
        assertEquals(1, insertCount);
        assertNotNull(user.getUserId());

        UserVO foundById = userMapper.findById(user.getUserId());
        assertNotNull(foundById);
        assertEquals(user.getEmail(), foundById.getEmail());
        assertEquals(user.getNickname(), foundById.getNickname());

        UserVO foundByEmail = userMapper.findByEmail(user.getEmail());
        assertNotNull(foundByEmail);
        assertEquals(user.getUserId(), foundByEmail.getUserId());

        UserVO foundByNickname = userMapper.findByNickname(user.getNickname());
        assertNotNull(foundByNickname);
        assertEquals(user.getUserId(), foundByNickname.getUserId());

        assertEquals(1, userMapper.countByEmail(user.getEmail()));
        assertEquals(1, userMapper.countByNickname(user.getNickname()));

        user.setNickname("mapper-test-updated");
        user.setPostalCode("54322");
        user.setAddressLine1("updated address");
        user.setAddressLine2("updated detail");
        assertEquals(1, userMapper.update(user));

        UserVO updated = userMapper.findById(user.getUserId());
        assertEquals("mapper-test-updated", updated.getNickname());
        assertEquals("54322", updated.getPostalCode());
        assertEquals("updated address", updated.getAddressLine1());
        assertEquals("updated detail", updated.getAddressLine2());

        user.setPassword("changed-password");
        assertEquals(1, userMapper.updatePassword(user));
        assertEquals("changed-password", userMapper.findById(user.getUserId()).getPassword());

        assertEquals(1, userMapper.deleteById(user.getUserId()));
        assertNull(userMapper.findById(user.getUserId()));
    }

    // 테스트용 사용자 값을 생성
    private UserVO createUser() {
        UserVO user = new UserVO();
        user.setEmail("mapper-test@example.com");
        user.setPassword("password");
        user.setNickname("mapper-test");
        user.setBirthDate(LocalDate.of(2001, 1, 1));
        user.setPostalCode("12346");
        user.setAddressLine1("test address");
        user.setAddressLine2("test detail");
        return user;
    }
}
