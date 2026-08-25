package org.kkobi.users.service;

import org.junit.jupiter.api.Test;
import org.kkobi.exception.DuplicateUserException;
import org.kkobi.users.domain.UserVO;
import org.kkobi.users.dto.request.ProfileUpdateRequest;
import org.kkobi.users.dto.response.UserInfoResponse;
import org.kkobi.users.enums.ProfileImageType;
import org.kkobi.users.mapper.UserMapper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceImplTest {

    @Test
    void getProfileReturnsLoggedInUserInformation() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.add(user(1L, "member@example.com", "member", LocalDate.of(2000, 1, 1)));
        UserService service = new UserServiceImpl(mapper, null);

        UserInfoResponse response = service.getProfile("member@example.com");

        assertEquals("member@example.com", response.getEmail());
        assertEquals("member", response.getNickname());
        assertEquals(LocalDate.of(2000, 1, 1), response.getBirthDate());
        assertEquals(ProfileImageType.SLEEP_KKOBI, response.getProfileImage());
    }

    @Test
    void updateProfileChangesOnlyNicknameAndBirthDate() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.add(user(1L, "member@example.com", "member", LocalDate.of(2000, 1, 1)));
        UserService service = new UserServiceImpl(mapper, null);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNickname("updated");
        request.setBirthDate(LocalDate.of(1999, 12, 31));
        request.setProfileImage(ProfileImageType.PROFILE_KKOBI_5);

        UserInfoResponse response = service.updateProfile("member@example.com", request);

        assertEquals("updated", response.getNickname());
        assertEquals(LocalDate.of(1999, 12, 31), response.getBirthDate());
        assertEquals(ProfileImageType.PROFILE_KKOBI_5, response.getProfileImage());
    }

    @Test
    void updateProfileUsesDefaultImageWhenLegacyRequestOmitsIt() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.add(user(1L, "member@example.com", "member", LocalDate.of(2000, 1, 1)));
        UserService service = new UserServiceImpl(mapper, null);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNickname("updated");
        request.setBirthDate(LocalDate.of(1999, 12, 31));

        UserInfoResponse response = service.updateProfile("member@example.com", request);

        assertEquals(ProfileImageType.SLEEP_KKOBI, response.getProfileImage());
    }

    @Test
    void updateProfileRejectsAnotherUsersNickname() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.add(user(1L, "member@example.com", "member", LocalDate.of(2000, 1, 1)));
        mapper.add(user(2L, "other@example.com", "taken", LocalDate.of(2001, 1, 1)));
        UserService service = new UserServiceImpl(mapper, null);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNickname("taken");
        request.setBirthDate(LocalDate.of(1999, 12, 31));

        assertThrows(
                DuplicateUserException.class,
                () -> service.updateProfile("member@example.com", request)
        );
    }

    private static UserVO user(Long id, String email, String nickname, LocalDate birthDate) {
        UserVO user = new UserVO();
        user.setUserId(id);
        user.setEmail(email);
        user.setNickname(nickname);
        user.setBirthDate(birthDate);
        return user;
    }

    private static class StubUserMapper implements UserMapper {
        private final Map<String, UserVO> usersByEmail = new HashMap<>();

        void add(UserVO user) {
            usersByEmail.put(user.getEmail(), user);
        }

        @Override
        public UserVO findByEmail(String email) {
            return usersByEmail.get(email);
        }

        @Override
        public UserVO findByNickname(String nickname) {
            return usersByEmail.values().stream()
                    .filter(user -> nickname.equals(user.getNickname()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public int updateProfile(
                Long userId,
                String nickname,
                LocalDate birthDate,
                ProfileImageType profileImage
        ) {
            UserVO user = usersByEmail.values().stream()
                    .filter(candidate -> userId.equals(candidate.getUserId()))
                    .findFirst()
                    .orElse(null);
            if (user == null) {
                return 0;
            }
            user.setNickname(nickname);
            user.setBirthDate(birthDate);
            user.setProfileImage(profileImage);
            return 1;
        }

        @Override public int insert(UserVO user) { throw new UnsupportedOperationException(); }
        @Override public UserVO findById(Long userId) { throw new UnsupportedOperationException(); }
        @Override public int countByEmail(String email) { throw new UnsupportedOperationException(); }
        @Override public int countByNickname(String nickname) { throw new UnsupportedOperationException(); }
        @Override public int update(UserVO user) { throw new UnsupportedOperationException(); }
        @Override public int updatePassword(UserVO user) { throw new UnsupportedOperationException(); }
        @Override public int deleteById(Long userId) { throw new UnsupportedOperationException(); }
    }
}
