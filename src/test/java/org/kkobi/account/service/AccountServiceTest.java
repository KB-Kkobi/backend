package org.kkobi.account.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.account.dto.AccountAssetInfoDto;
import org.kkobi.account.dto.AccountAssetStatusResponseDto;
import org.kkobi.account.mapper.AccountMapper;
import org.kkobi.assessment.service.GameAssessmentService;
import org.kkobi.product.holding.dto.response.SavingsAssetStatusResponseDto;
import org.kkobi.product.holding.service.ProductHoldingService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountServiceTest {

    @Test
    @DisplayName("계좌와 예적금 평가금액을 합산해 전체 자산 비중을 반환한다")
    void getAccountAssetStatus() {
        AccountAssetInfoDto account = new AccountAssetInfoDto();
        account.setAccountId(1L);
        account.setSeedMoney(new BigDecimal("10000000"));
        account.setCashBalance(new BigDecimal("6000000"));
        account.setStockAsset(new BigDecimal("2000000"));

        SavingsAssetStatusResponseDto savings =
                new SavingsAssetStatusResponseDto();
        savings.setTotalAfterTaxCurrentValue(new BigDecimal("2000000"));

        AccountService service = new AccountService(
                new StubAccountMapper(account),
                new StubProductHoldingService(savings),
                null
        );

        AccountAssetStatusResponseDto response =
                service.getAccountAssetStatus(10L);

        assertEquals(0, new BigDecimal("10000000")
                .compareTo(response.getTotalAsset()));
        assertEquals(0, BigDecimal.ZERO
                .compareTo(response.getTotalProfit()));
        assertEquals(0, new BigDecimal("60.00")
                .compareTo(response.getCashRatio()));
        assertEquals(0, new BigDecimal("20.00")
                .compareTo(response.getStockRatio()));
        assertEquals(0, new BigDecimal("20.00")
                .compareTo(response.getSavingsRatio()));
    }

    @Test
    @DisplayName("가상투자 계좌가 없으면 조회할 수 없다")
    void rejectMissingAccount() {
        AccountService service = new AccountService(
                new StubAccountMapper(null),
                new StubProductHoldingService(
                        new SavingsAssetStatusResponseDto()
                ),
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getAccountAssetStatus(10L)
        );
    }

    private static class StubAccountMapper implements AccountMapper {

        private final AccountAssetInfoDto account;

        private StubAccountMapper(AccountAssetInfoDto account) {
            this.account = account;
        }

        @Override
        public boolean existsAccountByUserId(Long userId) {
            return account != null;
        }

        @Override
        public int saveAccount(Long userId, BigDecimal seedMoney) {
            return 1;
        }

        @Override
        public AccountAssetInfoDto getAccountAssetInfoByUserId(Long userId) {
            return account;
        }
    }

    private static class StubProductHoldingService
            extends ProductHoldingService {

        private final SavingsAssetStatusResponseDto savings;

        private StubProductHoldingService(
                SavingsAssetStatusResponseDto savings
        ) {
            super(null, null, null);
            this.savings = savings;
        }

        @Override
        public SavingsAssetStatusResponseDto getSavingAssetStatus(Long userId) {
            return savings;
        }
    }

    @Test
    @DisplayName("성향파악게임을 완료하지 않으면 계좌를 생성할 수 없다")
    void rejectAccountCreationBeforeGameCompletion() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        GameAssessmentService gameAssessmentService =
                mock(GameAssessmentService.class);

        when(gameAssessmentService.existsCompletedGame(10L))
                .thenReturn(false);

        AccountService service = new AccountService(
                accountMapper,
                null,
                gameAssessmentService
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createAccount(10L)
        );

        assertEquals(
                "성향 파악 게임을 완료한 후 계좌를 생성할 수 있습니다.",
                exception.getMessage()
        );

        verify(accountMapper, never())
                .saveAccount(anyLong(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("성향파악게임을 완료하면 계좌를 생성할 수 있다")
    void createAccountAfterGameCompletion() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        GameAssessmentService gameAssessmentService =
                mock(GameAssessmentService.class);

        when(gameAssessmentService.existsCompletedGame(10L))
                .thenReturn(true);
        when(accountMapper.existsAccountByUserId(10L))
                .thenReturn(false);
        when(accountMapper.saveAccount(
                eq(10L),
                eq(BigDecimal.valueOf(5_000_000L))
        )).thenReturn(1);

        AccountService service = new AccountService(
                accountMapper,
                null,
                gameAssessmentService
        );

        service.createAccount(10L);

        verify(accountMapper, times(1))
                .saveAccount(
                        10L,
                        BigDecimal.valueOf(5_000_000L)
                );
    }
}
