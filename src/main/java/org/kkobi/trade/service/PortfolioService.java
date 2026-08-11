package org.kkobi.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.kkobi.external.kis.dto.StockPriceResponse;
import org.kkobi.external.kis.service.StockQuoteService;
import org.kkobi.trade.dto.HoldingDto;
import org.kkobi.trade.dto.TradeAccountDto;
import org.kkobi.trade.dto.TradeStockDto;
import org.kkobi.trade.dto.response.HoldingItemResponse;
import org.kkobi.trade.dto.response.HoldingsResponse;
import org.kkobi.trade.dto.response.PortfolioResponse;
import org.kkobi.trade.mapper.HoldingMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.kkobi.trade.mapper.TradeStockMapper;
import org.kkobi.trade.policy.MarketHoursPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class PortfolioService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TradeAccountMapper accountMapper;
    private final HoldingMapper holdingMapper;
    private final TradeStockMapper stockMapper;
    private final StockQuoteService stockQuoteService;
    private final MarketHoursPolicy marketHoursPolicy;

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long userId) {
        TradeAccountDto account = findAccount(userId);
        List<HoldingDto> holdings = holdingMapper.findAllByAccountId(account.getAccountId());

        long stockValuationAmount = 0L;
        long stockPrincipal = 0L;
        OffsetDateTime quotedAt = OffsetDateTime.now(KST);

        for (HoldingDto h : holdings) {
            if (h.getQuantity() == null || h.getQuantity() == 0) {
                continue;
            }
            long principal = h.getAveragePrice() * h.getQuantity();
            stockPrincipal += principal;

            Long currentPrice = fetchCurrentPriceQuietly(h.getSecurityId());
            if (currentPrice != null) {
                stockValuationAmount += currentPrice * h.getQuantity();
            } else {
                // 시세 실패 시 매입원가로 대체
                stockValuationAmount += principal;
            }
        }

        long cashBalance = account.getCashBalance();
        long lockedCash = account.getLockedCash();
        long orderableCash = cashBalance - lockedCash;
        long totalAsset = cashBalance + stockValuationAmount;
        long stockProfit = stockValuationAmount - stockPrincipal;
        double stockProfitRate = stockPrincipal == 0 ? 0.0
                : round2((double) stockProfit / stockPrincipal * 100);
        long seedMoney = account.getSeedMoney();
        long totalProfit = totalAsset - seedMoney;
        double totalProfitRate = seedMoney == 0 ? 0.0
                : round2((double) totalProfit / seedMoney * 100);

        return PortfolioResponse.builder()
                .totalAsset(totalAsset)
                .cashBalance(cashBalance)
                .lockedCash(lockedCash)
                .orderableCash(orderableCash)
                .stockValuationAmount(stockValuationAmount)
                .stockPrincipal(stockPrincipal)
                .stockProfit(stockProfit)
                .stockProfitRate(stockProfitRate)
                .seedMoney(seedMoney)
                .totalInvestedPrincipal(seedMoney)
                .totalProfit(totalProfit)
                .totalProfitRate(totalProfitRate)
                .quotedAt(quotedAt)
                .build();
    }

    @Transactional(readOnly = true)
    public HoldingsResponse getHoldings(Long userId) {
        TradeAccountDto account = findAccount(userId);
        List<HoldingDto> holdings = holdingMapper.findAllByAccountId(account.getAccountId());

        List<HoldingItemResponse> items = new ArrayList<>();
        OffsetDateTime quotedAt = OffsetDateTime.now(KST);

        for (HoldingDto h : holdings) {
            if (h.getQuantity() == null || h.getQuantity() == 0) {
                continue;
            }
            TradeStockDto stock = stockMapper.findById(h.getSecurityId());
            if (stock == null) {
                continue;
            }

            long principalAmount = h.getAveragePrice() * h.getQuantity();
            int sellableQuantity = h.getQuantity() - h.getLockedQuantity();

            StockPriceResponse quote = fetchQuoteQuietly(stock.getKisCode());
            HoldingItemResponse item = buildHoldingItem(h, stock, principalAmount, sellableQuantity, quote);
            items.add(item);
        }

        // valuationAmount 내림차순 정렬 (null이면 뒤로)
        items.sort((a, b) -> {
            Long va = a.getValuationAmount();
            Long vb = b.getValuationAmount();
            if (va == null && vb == null) return 0;
            if (va == null) return 1;
            if (vb == null) return -1;
            return Long.compare(vb, va);
        });

        return HoldingsResponse.builder()
                .holdings(items)
                .quotedAt(quotedAt)
                .build();
    }

    private HoldingItemResponse buildHoldingItem(
            HoldingDto h, TradeStockDto stock,
            long principalAmount, int sellableQuantity,
            StockPriceResponse quote) {

        if (quote == null || quote.price() == null) {
            return HoldingItemResponse.builder()
                    .holdingSecurityId(h.getHoldingSecurityId())
                    .securityId(h.getSecurityId())
                    .ticker(stock.getTicker())
                    .name(stock.getName())
                    .market(stock.getMarket())
                    .quantity(h.getQuantity())
                    .lockedQuantity(h.getLockedQuantity())
                    .sellableQuantity(sellableQuantity)
                    .averagePrice(h.getAveragePrice())
                    .principalAmount(principalAmount)
                    .currentPrice(null)
                    .valuationAmount(null)
                    .profit(null)
                    .profitRate(null)
                    .previousClose(null)
                    .changeAmount(null)
                    .changeRate(null)
                    .build();
        }

        long currentPrice = quote.price().longValue();
        long valuationAmount = currentPrice * h.getQuantity();
        long profit = valuationAmount - principalAmount;
        double profitRate = h.getAveragePrice() == 0 ? 0.0
                : round2((double) (currentPrice - h.getAveragePrice()) / h.getAveragePrice() * 100);

        long previousClose = quote.prevClose() != null ? quote.prevClose().longValue() : currentPrice;
        long changeAmount = currentPrice - previousClose;
        double changeRate = previousClose == 0 ? 0.0
                : round2((double) changeAmount / previousClose * 100);

        return HoldingItemResponse.builder()
                .holdingSecurityId(h.getHoldingSecurityId())
                .securityId(h.getSecurityId())
                .ticker(stock.getTicker())
                .name(stock.getName())
                .market(stock.getMarket())
                .quantity(h.getQuantity())
                .lockedQuantity(h.getLockedQuantity())
                .sellableQuantity(sellableQuantity)
                .averagePrice(h.getAveragePrice())
                .principalAmount(principalAmount)
                .currentPrice(currentPrice)
                .valuationAmount(valuationAmount)
                .profit(profit)
                .profitRate(profitRate)
                .previousClose(previousClose)
                .changeAmount(changeAmount)
                .changeRate(changeRate)
                .build();
    }

    private Long fetchCurrentPriceQuietly(Long securityId) {
        try {
            TradeStockDto stock = stockMapper.findById(securityId);
            if (stock == null || stock.getKisCode() == null) {
                return null;
            }
            StockPriceResponse resp = stockQuoteService.getCurrentPrice(stock.getKisCode());
            return resp != null && resp.price() != null ? resp.price().longValue() : null;
        } catch (Exception e) {
            log.warn("시세 조회 실패 securityId={} err={}", securityId, e.getMessage());
            return null;
        }
    }

    private StockPriceResponse fetchQuoteQuietly(String kisCode) {
        if (kisCode == null || kisCode.isBlank()) {
            return null;
        }
        try {
            return stockQuoteService.getCurrentPrice(kisCode);
        } catch (Exception e) {
            log.warn("시세 조회 실패 kisCode={} err={}", kisCode, e.getMessage());
            return null;
        }
    }

    private TradeAccountDto findAccount(Long userId) {
        log.info("[DEBUG] findAccount userId={}", userId);
        TradeAccountDto account = accountMapper.findByUserId(userId);
        log.info("[DEBUG] findAccount result={}", account);
        if (account == null) {
            throw new IllegalStateException("계좌가 없습니다.");
        }
        return account;
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
