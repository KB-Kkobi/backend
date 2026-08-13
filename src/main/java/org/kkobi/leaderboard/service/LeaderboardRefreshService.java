package org.kkobi.leaderboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.kkobi.leaderboard.dto.LeaderboardAccountDto;
import org.kkobi.leaderboard.dto.LeaderboardCacheDto;
import org.kkobi.leaderboard.dto.LeaderboardSecurityHoldingDto;
import org.kkobi.leaderboard.mapper.LeaderboardMapper;
import org.kkobi.product.holding.dto.ProductHoldingInfoDto;
import org.kkobi.product.holding.service.ProductHoldingService;
import org.kkobi.securities.dto.request.QuoteRequest;
import org.kkobi.securities.dto.response.QuoteItem;
import org.kkobi.securities.dto.response.QuoteResponse;
import org.kkobi.securities.service.SecurityQuoteService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class LeaderboardRefreshService {

    private final LeaderboardMapper leaderboardMapper;
    private final SecurityQuoteService securityQuoteService;
    private final ProductHoldingService productHoldingService;
    private final LeaderboardRedisService leaderboardRedisService;

    // 전체 리더보드 계산 데이터를 갱신
    public void refreshAll() {
        List<LeaderboardAccountDto> accounts =
                leaderboardMapper.getLeaderboardAccounts();

        List<LeaderboardSecurityHoldingDto> securityHoldings =
                leaderboardMapper.getSecurityHoldings();

        List<ProductHoldingInfoDto> productHoldings =
                leaderboardMapper.getActiveProductHoldings();

        Map<String, BigDecimal> priceByTicker =
                getCurrentPrices(securityHoldings);

        Map<Long, BigDecimal> stockAssetByUser =
                calculateStockAssets(securityHoldings, priceByTicker);

        Map<Long, BigDecimal> productAssetByUser =
                calculateProductAssets(
                        accounts,
                        productHoldings
                );

        saveLeaderboardCaches(
                accounts,
                stockAssetByUser,
                productAssetByUser
        );

        log.info(
                "리더보드 주식 자산 계산 완료 account={} holdings={} users={}",
                accounts.size(),
                securityHoldings.size(),
                stockAssetByUser.size()
        );

    }
    // 보유 종목의 현재가를 한 번에 조회
    private Map<String, BigDecimal> getCurrentPrices(
            List<LeaderboardSecurityHoldingDto> holdings
    ) {
        Set<String> tickerSet = new LinkedHashSet<>();

        for(LeaderboardSecurityHoldingDto holding : holdings){
            if(holding.getTicker() != null){
                tickerSet.add(holding.getTicker());
            }
        }

        if(tickerSet.isEmpty()){
            return new HashMap<>();
        }

        QuoteRequest request = new QuoteRequest();
        request.setTickers(new ArrayList<>(tickerSet));

         QuoteResponse response =
                 securityQuoteService.getQuotes(request);

         Map<String, BigDecimal> priceByTicker = new HashMap<>();

         for(QuoteItem quote : response.quotes()){
             if(quote.price() != null) {
                 priceByTicker.put(
                         quote.ticker(),
                         quote.price()
                 );
             }
         }

         return  priceByTicker;
    }

    // 사용자별 주식 평가액을 계산
    private Map<Long, BigDecimal> calculateStockAssets(
            List<LeaderboardSecurityHoldingDto> holdings,
            Map<String, BigDecimal> priceByTicker
    ) {
        Map<Long,BigDecimal> stockAssetByUser = new HashMap<>();

        for(LeaderboardSecurityHoldingDto holding : holdings){
            BigDecimal currentPrice =
                    priceByTicker.get(holding.getTicker());

            if(currentPrice == null || holding.getQuantity() == null){
                continue;
            }

            BigDecimal stockValue = currentPrice.multiply(
                    BigDecimal.valueOf(holding.getQuantity())
            );

            stockAssetByUser.merge(
                    holding.getUserId(),
                    stockValue,
                    BigDecimal::add
            );
        }

        return stockAssetByUser;
    }

    // 사용자별 예적금 평가금액을 계산
    private Map<Long, BigDecimal> calculateProductAssets(
            List<LeaderboardAccountDto> accounts,
            List<ProductHoldingInfoDto> productHoldings
    ) {
        Map<Long, Long> userIdByAccountId = new HashMap<>();

        for(LeaderboardAccountDto account : accounts) {
            userIdByAccountId.put(
                    account.getAccountId(),
                    account.getUserId()
            );
        }

        Map<Long, BigDecimal> productAssetByUser =
                new HashMap<>();

        for(ProductHoldingInfoDto holding : productHoldings){
            Long userId = userIdByAccountId.get(holding.getAccountId());

            if(userId == null){
                continue;
            }

            BigDecimal currentValue =
                    productHoldingService.calculateAfterTaxCurrentValue(holding);

            productAssetByUser.merge(
                    userId,
                    currentValue,
                    BigDecimal::add
            );
        }

        return productAssetByUser;
    }

    // 사용자별 리더보드 데이터를 계산하고 Redis에 저장
    private void saveLeaderboardCaches(
            List<LeaderboardAccountDto> accounts,
            Map<Long, BigDecimal> stockAssetByUser,
            Map<Long, BigDecimal> productAssetByUser
    ) {
        for(LeaderboardAccountDto account : accounts) {
            Long userId = account. getUserId();

            BigDecimal cashBalance =
                    account.getCashBalance() != null
                    ? account.getCashBalance()
                            : BigDecimal.ZERO;

            BigDecimal stockAsset =
                    stockAssetByUser.getOrDefault(userId, BigDecimal.ZERO);

            BigDecimal productAsset =
                    productAssetByUser.getOrDefault(userId, BigDecimal.ZERO);

            BigDecimal totalAsset = cashBalance
                    .add(stockAsset)
                    .add(productAsset);

            BigDecimal returnRate =
                    calculateReturnRate(
                            totalAsset,
                            account.getSeedMoney()
                    );

            LeaderboardCacheDto cacheDto = new LeaderboardCacheDto();

            cacheDto.setUserId(userId);
            cacheDto.setNickname(account.getNickname());
            cacheDto.setPersonaId(account.getPersonaId());
            cacheDto.setPersonaName(account.getPersonaName());
            cacheDto.setTotalAsset(totalAsset);
            cacheDto.setReturnRate(returnRate);

            leaderboardRedisService.saveUserCache(cacheDto);

            leaderboardRedisService.savePersonaRanking(
                    account.getPersonaId(),
                    userId,
                    returnRate
            );
        }
    }

    // 초기 투자금 대비 수익률을 계산
    private BigDecimal calculateReturnRate(
            BigDecimal totalAsset,
            BigDecimal seedMoney
    ) {
        if(seedMoney == null || seedMoney.compareTo(BigDecimal.ZERO) <= 0){
            return BigDecimal.ZERO;
        }

        return totalAsset
                .subtract(seedMoney)
                .divide(
                        seedMoney,
                        6,
                        RoundingMode.HALF_UP
                )
                .multiply(BigDecimal.valueOf(100))
                .setScale(2,RoundingMode.HALF_UP);
    }
}
