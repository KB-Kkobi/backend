package org.kkobi.security.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JsonResponse {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    // 객체를 JSON HTTP 응답으로 작성
    public static <T> void send(HttpServletResponse response, T result) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(result));
        response.getWriter().flush();
    }

    // 에러 상태 코드와 메시지를 HTTP 응답으로 작성
    public static void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(JSON_CONTENT_TYPE);
        response.getWriter().write(message);
        response.getWriter().flush();
    }
}
