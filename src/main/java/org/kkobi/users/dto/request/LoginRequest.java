package org.kkobi.users.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.regex.Pattern;

@Data
public class LoginRequest {
    // 이메일의 기본 형식(아이디@도메인)을 검사하기 위한 정규식
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$"
    );

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    private String password;

    public static LoginRequest of(HttpServletRequest request) throws IOException {
        LoginRequest login = new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);
        login.validate();
        return login;
    }

    // 로그인 요청의 필수 값과 이메일 형식을 검사하고 이메일 양쪽 공백을 제거
    private void validate() throws IOException {
        if (email == null || email.trim().isEmpty()) {
            throw new IOException("이메일은 필수 입력 값입니다.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IOException("올바른 이메일 형식이 아닙니다.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IOException("비밀번호는 필수 입력 값입니다.");
        }

        email = email.trim();
    }
}
