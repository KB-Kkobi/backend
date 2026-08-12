package org.kkobi.trade.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.exception.CommonExceptionAdvice;
import org.kkobi.security.principal.CustomUserDetails;
import org.kkobi.trade.dto.request.OrderListRequest;
import org.kkobi.trade.dto.response.OrderListResponse;
import org.kkobi.trade.service.OrderQueryService;
import org.kkobi.trade.service.OrderService;
import org.kkobi.users.domain.UserVO;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TradeOrderControllerGetOrdersTest {

    private MockMvc mvc;

    @Mock
    private OrderQueryService orderQueryService;
    @Mock
    private OrderService orderService;

    private static final Long USER_ID = 1L;

    private static final OrderListResponse EMPTY_RESPONSE = OrderListResponse.builder()
            .orders(List.of())
            .page(0)
            .size(20)
            .totalElements(0)
            .hasNext(false)
            .build();

    @BeforeEach
    void setUp() {
        TradeOrderController controller = new TradeOrderController(orderService, orderQueryService);

        // ISO 날짜 포맷(yyyy-MM-dd) 전역 등록 — ServletConfig.addFormatters 와 동일 설정
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setUseIsoFormat(true);
        registrar.registerFormatters(conversionService);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setConversionService(conversionService)
                .setControllerAdvice(new CommonExceptionAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        UserVO userVO = new UserVO();
        userVO.setUserId(USER_ID);
        userVO.setEmail("test@test.com");
        userVO.setPassword("pw");
        CustomUserDetails userDetails = new CustomUserDetails(userVO);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList())
        );

        lenient().when(orderQueryService.getOrders(eq(USER_ID), any(OrderListRequest.class)))
                .thenReturn(EMPTY_RESPONSE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── 정상 케이스 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("from 만 지정 — 200")
    void fromOnly() throws Exception {
        mvc.perform(get("/api/orders").param("from", "2026-07-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("from + to 지정 — 200")
    void fromAndTo() throws Exception {
        mvc.perform(get("/api/orders")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("from, to 둘 다 생략 — 200")
    void noDateParams() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("sort=desc 포함 — 200 (지원되는 파라미터)")
    void withSortDesc() throws Exception {
        mvc.perform(get("/api/orders")
                        .param("from", "2026-07-11")
                        .param("sort", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("명세에 없는 임의 파라미터 포함 — 200 (서버가 무시)")
    void unknownParamIsIgnored() throws Exception {
        mvc.perform(get("/api/orders")
                        .param("from", "2026-07-11")
                        .param("unknownParam", "whatever"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── 오류 케이스 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("from > to 인 경우 — 400 (서비스 검증)")
    void fromAfterTo() throws Exception {
        when(orderQueryService.getOrders(eq(USER_ID), any(OrderListRequest.class)))
                .thenThrow(new IllegalArgumentException("from 날짜는 to 날짜보다 이후일 수 없습니다."));

        mvc.perform(get("/api/orders")
                        .param("from", "2026-07-31")
                        .param("to", "2026-07-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 형식 2026-7-1 (단자리 월/일) — 400 + 에러 메시지")
    void invalidFormat_singleDigitMonthDay() throws Exception {
        mvc.perform(get("/api/orders").param("from", "2026-7-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from 파라미터 값이 올바르지 않습니다: 2026-7-1"));
    }

    @Test
    @DisplayName("잘못된 형식 20260711 (구분자 없음) — 400 + 에러 메시지")
    void invalidFormat_noHyphen() throws Exception {
        mvc.perform(get("/api/orders").param("from", "20260711"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from 파라미터 값이 올바르지 않습니다: 20260711"));
    }
}
