package org.kkobi.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkobi.external.kis.dto.StockPriceResponse;
import org.kkobi.external.kis.service.StockQuoteService;
import org.kkobi.trade.dto.CancelOrderResult;
import org.kkobi.trade.dto.HoldingDto;
import org.kkobi.trade.dto.OrderDto;
import org.kkobi.trade.dto.PlaceOrderRequest;
import org.kkobi.trade.dto.PlaceOrderResult;
import org.kkobi.trade.dto.TradeAccountDto;
import org.kkobi.trade.dto.TradeStockDto;
import org.kkobi.trade.enums.OrderMethod;
import org.kkobi.trade.enums.OrderStatus;
import org.kkobi.trade.enums.OrderType;
import org.kkobi.trade.event.SecurityOrderFilledEvent;
import org.kkobi.trade.exception.TradeErrorCode;
import org.kkobi.trade.exception.TradeException;
import org.kkobi.trade.mapper.HoldingMapper;
import org.kkobi.trade.mapper.OrderMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.kkobi.trade.mapper.TradeStockMapper;
import org.kkobi.trade.policy.MarketHoursPolicy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final TradeAccountMapper accountMapper;
    private final HoldingMapper holdingMapper;
    private final OrderMapper orderMapper;
    private final TradeStockMapper stockMapper;
    private final StockQuoteService stockQuoteService;
    private final MarketHoursPolicy marketHoursPolicy;
    private final OrderExecutionService executionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PlaceOrderResult placeOrder(Long userId, PlaceOrderRequest req) {
        if (!marketHoursPolicy.isMarketOpen()) {
            throw new TradeException(TradeErrorCode.MARKET_CLOSED);
        }

        if (req.getQuantity() == null || req.getQuantity() < 1) {
            throw new TradeException(TradeErrorCode.INVALID_QUANTITY);
        }
        if (req.getOrderMethod() == OrderMethod.LIMIT) {
            if (req.getPrice() == null) {
                throw new TradeException(TradeErrorCode.PRICE_REQUIRED_FOR_LIMIT);
            }
            if (req.getPrice() <= 0) {
                throw new TradeException(TradeErrorCode.INVALID_PRICE);
            }
        }

        TradeStockDto stock = stockMapper.findById(req.getSecurityId());
        if (stock == null) {
            throw new TradeException(TradeErrorCode.SECURITY_NOT_FOUND);
        }

        StockPriceResponse priceResp = fetchStockPrice(stock.getKisCode());
        long currentPrice = priceResp.price().longValue();

        TradeAccountDto account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new IllegalStateException("계좌가 없습니다.");
        }
        accountMapper.findByAccountIdForUpdate(account.getAccountId());

        return switch (req.getOrderMethod()) {
            case MARKET -> placeMarketOrder(account, stock, req, currentPrice, priceResp, userId);
            case LIMIT  -> placeLimitOrder(account, stock, req, currentPrice, priceResp, userId);
        };
    }

    private PlaceOrderResult placeMarketOrder(
            TradeAccountDto account, TradeStockDto stock,
            PlaceOrderRequest req, long currentPrice,
            StockPriceResponse priceResp, Long userId) {

        int qty = req.getQuantity();
        long amount = currentPrice * qty;

        if (req.getOrderType() == OrderType.BUY) {
            long orderable = account.getCashBalance() - account.getLockedCash();
            if (orderable < amount) {
                throw new TradeException(TradeErrorCode.INSUFFICIENT_CASH);
            }
        } else {
            validateSellable(account.getAccountId(), stock.getSecurityId(), qty);
        }

        OrderDto order = buildOrder(account.getAccountId(), stock.getSecurityId(), req, null);
        order.setStatus(OrderStatus.PENDING);
        orderMapper.insert(order);
        executionService.execute(order, currentPrice, false);
        publishFilledEvent(userId, account, stock, order, currentPrice, priceResp);

        return buildResult(order, stock, currentPrice, OrderStatus.FILLED);
    }

    private PlaceOrderResult placeLimitOrder(
            TradeAccountDto account, TradeStockDto stock,
            PlaceOrderRequest req, long currentPrice,
            StockPriceResponse priceResp, Long userId) {

        long limitPrice = req.getPrice();
        int qty = req.getQuantity();

        boolean immediatelyFilled =
                (req.getOrderType() == OrderType.BUY  && limitPrice >= currentPrice) ||
                (req.getOrderType() == OrderType.SELL && limitPrice <= currentPrice);

        if (req.getOrderType() == OrderType.BUY) {
            long requiredCash = limitPrice * qty;
            long orderable = account.getCashBalance() - account.getLockedCash();
            if (orderable < requiredCash) {
                throw new TradeException(TradeErrorCode.INSUFFICIENT_CASH);
            }
        } else {
            validateSellable(account.getAccountId(), stock.getSecurityId(), qty);
        }

        OrderDto order = buildOrder(account.getAccountId(), stock.getSecurityId(), req, limitPrice);

        if (immediatelyFilled) {
            order.setStatus(OrderStatus.PENDING);
            orderMapper.insert(order);
            executionService.execute(order, currentPrice, false);
            publishFilledEvent(userId, account, stock, order, currentPrice, priceResp);
            return buildResult(order, stock, currentPrice, OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PENDING);
            orderMapper.insert(order);
            if (req.getOrderType() == OrderType.BUY) {
                accountMapper.increaseLocked(account.getAccountId(), limitPrice * qty);
            } else {
                HoldingDto h = holdingMapper.findByAccountAndSecurity(
                        account.getAccountId(), stock.getSecurityId());
                holdingMapper.increaseLocked(h.getHoldingSecurityId(), qty);
            }
            return buildResult(order, stock, null, OrderStatus.PENDING);
        }
    }

    @Transactional
    public CancelOrderResult cancelOrder(Long userId, Long securityOrderId) {
        OrderDto order = orderMapper.findById(securityOrderId);
        if (order == null) {
            throw new TradeException(TradeErrorCode.ORDER_NOT_FOUND);
        }

        TradeAccountDto account = accountMapper.findByUserId(userId);
        if (account == null || !account.getAccountId().equals(order.getAccountId())) {
            throw new TradeException(TradeErrorCode.FORBIDDEN_ORDER);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new TradeException(TradeErrorCode.ORDER_NOT_CANCELABLE);
        }

        accountMapper.findByAccountIdForUpdate(account.getAccountId());

        int updated = orderMapper.updateCancelled(securityOrderId);
        if (updated == 0) {
            throw new TradeException(TradeErrorCode.ORDER_NOT_CANCELABLE);
        }

        if (order.getOrderType() == OrderType.BUY) {
            accountMapper.decreaseLocked(account.getAccountId(),
                    order.getOrderPrice() * order.getQuantity());
        } else {
            HoldingDto h = holdingMapper.findByAccountAndSecurity(
                    account.getAccountId(), order.getSecurityId());
            if (h != null) {
                holdingMapper.decreaseLocked(h.getHoldingSecurityId(), order.getQuantity());
            }
        }

        CancelOrderResult result = new CancelOrderResult();
        result.setSecurityOrderId(securityOrderId);
        result.setStatus(OrderStatus.CANCELLED);
        result.setUpdatedAt(LocalDateTime.now());
        return result;
    }

    private void publishFilledEvent(Long userId, TradeAccountDto account, TradeStockDto stock,
                                    OrderDto order, long currentPrice, StockPriceResponse priceResp) {
        BigDecimal open = priceResp.open();
        BigDecimal high = priceResp.high();
        BigDecimal low = priceResp.low();
        if (open == null || high == null || low == null
                || open.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("시가 정보 없음, 성향 이벤트 생략 securityOrderId={}", order.getSecurityOrderId());
            return;
        }
        BigDecimal dailyPriceRangeRate = high.subtract(low)
                .divide(open, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        SecurityOrderFilledEvent event = new SecurityOrderFilledEvent(
                userId,
                account.getAccountId(),
                order.getSecurityOrderId(),
                order.getOrderType().name(),
                stock.getSecurityId(),
                stock.getTicker(),
                order.getQuantity(),
                currentPrice * order.getQuantity(),
                LocalDateTime.now(),
                priceResp.changeRate(),
                dailyPriceRangeRate
        );
        eventPublisher.publishEvent(event);
    }

    private StockPriceResponse fetchStockPrice(String kisCode) {
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

    private void validateSellable(Long accountId, Long securityId, int qty) {
        HoldingDto h = holdingMapper.findByAccountAndSecurity(accountId, securityId);
        if (h == null) {
            throw new TradeException(TradeErrorCode.INSUFFICIENT_QUANTITY);
        }
        int sellable = h.getQuantity() - h.getLockedQuantity();
        if (sellable < qty) {
            throw new TradeException(TradeErrorCode.INSUFFICIENT_QUANTITY);
        }
    }

    private OrderDto buildOrder(Long accountId, Long securityId,
                                PlaceOrderRequest req, Long orderPrice) {
        OrderDto o = new OrderDto();
        o.setAccountId(accountId);
        o.setSecurityId(securityId);
        o.setOrderType(req.getOrderType());
        o.setOrderMethod(req.getOrderMethod());
        o.setOrderPrice(orderPrice);
        o.setQuantity(req.getQuantity());
        return o;
    }

    private PlaceOrderResult buildResult(OrderDto order, TradeStockDto stock,
                                         Long executedPrice, OrderStatus status) {
        PlaceOrderResult r = new PlaceOrderResult();
        r.setSecurityOrderId(order.getSecurityOrderId());
        r.setSecurityId(stock.getSecurityId());
        r.setTicker(stock.getTicker());
        r.setName(stock.getName());
        r.setOrderType(order.getOrderType());
        r.setOrderMethod(order.getOrderMethod());
        r.setOrderPrice(order.getOrderPrice());
        r.setExecutedPrice(executedPrice);
        r.setQuantity(order.getQuantity());
        r.setExecutedAmount(executedPrice != null ? executedPrice * order.getQuantity() : null);
        r.setStatus(status);
        r.setOrderedAt(order.getOrderedAt());
        r.setExecutedAt(executedPrice != null ? LocalDateTime.now() : null);
        return r;
    }
}
