package org.kkobi.assessment.controller;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.dto.AssessmentResultResponseDto;
import org.kkobi.assessment.service.AssessmentResultService;
import org.kkobi.exception.CommonExceptionAdvice;
import org.kkobi.security.principal.CustomUserDetails;
import org.kkobi.persona.dto.PersonaResponseDto;
import org.kkobi.users.domain.UserVO;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AssessmentControllerTest {

    private MockMvc mvc;

    @Mock
    private AssessmentResultService assessmentResultService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        AssessmentController controller = new AssessmentController(assessmentResultService);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(
                Jackson2ObjectMapperBuilder.json()
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build()
        );

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CommonExceptionAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(converter)
                .build();

        UserVO userVO = new UserVO();
        userVO.setUserId(USER_ID);
        userVO.setEmail("test@test.com");
        userVO.setPassword("pw");
        CustomUserDetails userDetails = new CustomUserDetails(userVO);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("진단 이력이 있으면 200과 결과를 반환한다.")
    void returnsOkWhenAssessmentExists() throws Exception {
        AssessmentResultDetails details = sampleDetails();
        when(assessmentResultService.getLatestAssessmentResultDetails(USER_ID))
                .thenReturn(details);

        mvc.perform(get("/api/assessments/me/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultId").value(42))
                .andExpect(jsonPath("$.personaCode").value("AA"))
                .andExpect(jsonPath("$.scores.rtScore").value(60.00))
                .andExpect(jsonPath("$.recommendedRatio.stockRatio").value(40.00));
    }

    @Test
    @DisplayName("진단 이력이 없으면 404와 메시지를 반환한다.")
    void returns404WhenNoAssessment() throws Exception {
        when(assessmentResultService.getLatestAssessmentResultDetails(USER_ID))
                .thenReturn(null);

        mvc.perform(get("/api/assessments/me/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("성향 진단 이력이 없습니다."));
    }

    @Test
    @DisplayName("진단 결과에 성향별 포트폴리오 배분 이유를 반환한다.")
    void returnsPortfolioReasonsForPersona() throws Exception {
        PersonaResponseDto persona = new PersonaResponseDto();
        persona.setPersonaName("불꽃 추격자");
        persona.setAxisCode("HHH");
        persona.setPortfolioReasonFirst(
                "가격이 크게 움직여도 수익 기회를 적극적으로 잡는 편이라 "
                        + "주식 비중을 80%로 높게 배분했어요."
        );
        persona.setPortfolioReasonSecond(
                "다만 필요할 때 자금을 움직일 수 있는 것도 중요하게 생각해, "
                        + "나머지 자산은 채권보다 예·적금 비중을 조금 더 높였어요."
        );
        persona.setStockRatio(new BigDecimal("80.00"));
        persona.setBondRatio(new BigDecimal("5.00"));
        persona.setDepositRatio(new BigDecimal("15.00"));

        AssessmentResultResponseDto result = new AssessmentResultResponseDto();
        result.setResultId(42L);
        result.setUserId(USER_ID);
        result.setPersona(persona);

        when(assessmentResultService.getLatestAssessmentResult(USER_ID))
                .thenReturn(result);

        mvc.perform(get("/api/assessments/me/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persona.axisCode").value("HHH"))
                .andExpect(jsonPath("$.persona.portfolioReasonFirst")
                        .value(persona.getPortfolioReasonFirst()))
                .andExpect(jsonPath("$.persona.portfolioReasonSecond")
                        .value(persona.getPortfolioReasonSecond()))
                .andExpect(jsonPath("$.persona.stockRatio").value(80.00))
                .andExpect(jsonPath("$.persona.bondRatio").value(5.00))
                .andExpect(jsonPath("$.persona.depositRatio").value(15.00));
    }

    private AssessmentResultDetails sampleDetails() {
        AssessmentResultDetails d = new AssessmentResultDetails();
        d.setResultId(42L);
        d.setAxisCode("AA");
        d.setPersonaName("안정형");
        d.setDescription("설명");
        d.setFeature("특징");
        d.setStrength("전략");
        d.setCaution("주의사항");
        d.setRtScore(new BigDecimal("60.00"));
        d.setLhScore(new BigDecimal("50.00"));
        d.setRpScore(new BigDecimal("40.00"));
        d.setStockRatio(new BigDecimal("40.00"));
        d.setBondRatio(new BigDecimal("30.00"));
        d.setDepositRatio(new BigDecimal("30.00"));
        return d;
    }
}
