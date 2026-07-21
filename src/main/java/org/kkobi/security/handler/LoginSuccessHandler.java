package org.kkobi.security.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.kkobi.security.account.domain.CustomUser;
import org.kkobi.security.account.dto.AuthResultDTO;
import org.kkobi.security.account.dto.UserInfoDTO;
import org.kkobi.security.util.JsonResponse;
import org.kkobi.security.util.JwtProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Log4j2
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProcessor jwtProcessor;

    private AuthResultDTO makeAuthResult(CustomUser user){
        String username = user.getUsername();

        String token = jwtProcessor.generateToken(username);

        return new AuthResultDTO(token, UserInfoDTO.of(user.getMember()));
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomUser user = (CustomUser) authentication.getPrincipal();

        AuthResultDTO result = makeAuthResult(user);
        JsonResponse.send(response, result);
    }
}
