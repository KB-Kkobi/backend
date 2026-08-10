package org.kkobi.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.kkobi.users.dto.request.LoginRequest;
import org.kkobi.users.dto.response.TokenResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    // Swagger 문서 정보와 JWT Bearer 인증 방식을 설정
    @Bean
    public OpenAPI openAPI() {
        Components components = new Components()
                .addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token 입력")
                );

        ModelConverters.getInstance()
                .read(LoginRequest.class)
                .forEach(components::addSchemas);

        ModelConverters.getInstance()
                .read(TokenResponse.class)
                .forEach(components::addSchemas);

        Operation loginOperation = new Operation()
                .tags(List.of("회원"))
                .summary("로그인")
                .description("이메일과 비밀번호로 로그인하고 JWT 토큰을 발급합니다.")
                .security(Collections.emptyList())
                .requestBody(
                        new RequestBody()
                                .required(true)
                                .content(
                                        new Content()
                                                .addMediaType(
                                                        "application/json",
                                                        new MediaType()
                                                                .schema(
                                                                        new Schema<>()
                                                                                .$ref("#/components/schemas/LoginRequest")
                                                                )
                                                )
                                )
                )
                .responses(
                        new ApiResponses()
                                .addApiResponse(
                                        "200",
                                        new ApiResponse()
                                                .description("로그인 성공")
                                                .content(
                                                        new Content()
                                                                .addMediaType(
                                                                        "application/json",
                                                                        new MediaType()
                                                                                .schema(
                                                                                        new Schema<>()
                                                                                                .$ref("#/components/schemas/TokenResponse")
                                                                                )
                                                                )
                                                )
                                )
                                .addApiResponse(
                                        "400",
                                        new ApiResponse()
                                                .description("잘못된 로그인 요청")
                                )
                                .addApiResponse(
                                        "401",
                                        new ApiResponse()
                                                .description("이메일 또는 비밀번호 불일치")
                                )
                );

        return new OpenAPI()
                .info(
                        new Info()
                                .title("KKOBI API")
                                .description("꼬비 백엔드 API 명세")
                                .version("v1")
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(SECURITY_SCHEME_NAME)
                )
                .components(components)
                .paths(
                        new Paths()
                                .addPathItem(
                                        "/api/auth/login",
                                        new PathItem().post(loginOperation)
                                )
                );
    }
}