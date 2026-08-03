package org.kkobi.users.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.IOException;

@Data
public class LoginRequest {
    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    private String password;

    public static LoginRequest of(HttpServletRequest request) throws IOException {
        return new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);
    }
}
