package org.kkobi.users.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Data
public class LoginRequest {
    private String email;
    private String password;

    public static LoginRequest of(HttpServletRequest request) throws IOException {
        return new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);
    }
}
