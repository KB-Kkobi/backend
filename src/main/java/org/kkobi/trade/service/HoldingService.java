package org.kkobi.trade.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.trade.dto.HoldingDto;
import org.kkobi.trade.exception.TradeErrorCode;
import org.kkobi.trade.exception.TradeException;
import org.kkobi.trade.mapper.HoldingMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HoldingService {

    private final HoldingMapper holdingMapper;

    /**
     * 매수 체결 시 보유 레코드 upsert + 이동평균 갱신
     * newAvg = floor((oldAvg * oldQty + executedPrice * executedQty) / (oldQty + executedQty))
     */
    public void applyBuy(Long accountId, Long securityId, Long executedPrice, int quantity) {
        HoldingDto existing = holdingMapper.findByAccountAndSecurity(accountId, securityId);
        if (existing == null) {
            HoldingDto h = new HoldingDto();
            h.setAccountId(accountId);
            h.setSecurityId(securityId);
            h.setQuantity(quantity);
            h.setLockedQuantity(0);
            h.setAveragePrice(executedPrice);
            holdingMapper.insert(h);
        } else {
            long newQty = (long) existing.getQuantity() + quantity;
            long newAvg = Math.floorDiv(
                    existing.getAveragePrice() * existing.getQuantity()
                            + executedPrice * (long) quantity,
                    newQty
            );
            holdingMapper.updateQuantityAndAvgPrice(
                    existing.getHoldingSecurityId(), (int) newQty, newAvg);
        }
    }

    /**
     * 매도 체결 시 수량 차감
     */
    public void applySell(Long accountId, Long securityId, int quantity) {
        HoldingDto existing = holdingMapper.findByAccountAndSecurity(accountId, securityId);
        if (existing == null || existing.getQuantity() < quantity) {
            throw new TradeException(TradeErrorCode.INSUFFICIENT_QUANTITY);
        }
        holdingMapper.decreaseQuantity(existing.getHoldingSecurityId(), quantity);
    }
}
