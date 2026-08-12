package org.kkobi.trade.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.external.kis.dto.StockPriceResponse;
import org.kkobi.external.kis.service.StockQuoteService;
import org.kkobi.trade.dto.HoldingDto;
import org.kkobi.trade.dto.TradeAccountDto;
import org.kkobi.trade.dto.TradeStockDto;
import org.kkobi.trade.dto.response.OrderableResponse;
import org.kkobi.trade.dto.response.SecurityQuoteResponse;
import org.kkobi.trade.exception.TradeErrorCode;
import org.kkobi.trade.exception.TradeException;
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

@Service
@RequiredArgsConstructor
public class TradeSecurityService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TradeStockMapper stockMapper;
    private final TradeAccountMapper accountMapper;
    private final HoldingMapper holdingMapper;
    private final StockQuoteService stockQuoteService;
    private final MarketHoursPolicy marketHoursPolicy;

    @Transactional(readOnly = true)
    public SecurityQuoteResponse getQuote(Long securityId) {
        TradeStockDto stock = stockMapper.findById(securityId);
        if (stock == null) {
            throw new TradeException(TradeErrorCode.SECURITY_NOT_FOUND);
        }

        StockPriceResponse quote = fetchQuoteOrThrow(stock.getKisCode());

        long currentPrice = quote.price().longValue();
        long previousClose = quote.prevClose() != null ? quote.prevClose().longValue() : currentPrice;
        long changeAmount = currentPrice - previousClose;
        double changeRate = previousClose == 0 ? 0.0
                : round2((double) changeAmount / previousClose * 100);

        return SecurityQuoteResponse.builder()
                .securityId(stock.getSecurityId())
                .ticker(stock.getTicker())
                .name(stock.getName())
                .market(stock.getMarket())
                .currentPrice(currentPrice)
                .previousClose(previousClose)
                .changeAmount(changeAmount)
                .changeRate(changeRate)
                .isMarketOpen(marketHoursPolicy.isMarketOpen())
                .quotedAt(OffsetDateTime.now(KST))
                .build();
    }

    @Transactional(readOnly = true)
    public OrderableResponse getOrderable(Long securityId, Long userId) {
        TradeStockDto stock = stockMapper.findById(securityId);
        if (stock == null) {
            throw new TradeException(TradeErrorCode.SECURITY_NOT_FOUND);
        }

        StockPriceResponse quote = fetchQuoteOrThrow(stock.getKisCode());
        long currentPrice = quote.price().longValue();

        TradeAccountDto account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new IllegalStateException("계좌가 없습니다.");
        }

        long orderableCash = account.getCashBalance() - account.getLockedCash();
        int maxBuyQuantityAtMarket = currentPrice == 0 ? 0
                : (int) (orderableCash / currentPrice);

        HoldingDto holding = holdingMapper.findByAccountAndSecurity(account.getAccountId(), securityId);
        int sellableQuantity = 0;
        if (holding != null && holding.getQuantity() != null) {
            sellableQuantity = holding.getQuantity() - holding.getLockedQuantity();
        }

        return OrderableResponse.builder()
                .securityId(stock.getSecurityId())
                .orderableCash(orderableCash)
                .sellableQuantity(sellableQuantity)
                .currentPrice(currentPrice)
                .maxBuyQuantityAtMarket(maxBuyQuantityAtMarket)
                .isMarketOpen(marketHoursPolicy.isMarketOpen())
                .build();
    }

    private StockPriceResponse fetchQuoteOrThrow(String kisCode) {
        if (kisCode == null || kisCode.isBlank()) {
            throw new TradeException(TradeErrorCode.QUOTE_UNAVAILABLE);
        }
        try {
            StockPriceResponse resp = stockQuoteService.getCurrentPrice(kisCode);
            if (resp == null || resp.price() == null) {
                throw new TradeException(TradeErrorCode.QUOTE_UNAVAILABLE);
            }
            return resp;
        } catch (TradeException e) {
            throw e;
        } catch (Exception e) {
            throw new TradeException(TradeErrorCode.QUOTE_UNAVAILABLE);
        }
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
