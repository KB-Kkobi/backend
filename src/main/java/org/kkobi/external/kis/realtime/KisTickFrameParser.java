package org.kkobi.external.kis.realtime;

import org.kkobi.external.kis.realtime.dto.StockTick;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// KIS 실시간 체결가(H0STCNT0) 파이프 구분 프레임을 StockTick 리스트로 파싱한다.
// 프레임 형식: "<암호화여부>|<TR_ID>|<데이터건수>|<데이터>"
//   - 데이터는 필드 사이 '^', 다중 레코드 사이 '+' 로 구분
//   - H0STCNT0 필드 순서(KIS 공식 스펙 발췌):
//     0 MKSC_SHRN_ISCD  1 STCK_CNTG_HOUR  2 STCK_PRPR       3 PRDY_VRSS_SIGN
//     4 PRDY_VRSS       5 PRDY_CTRT       6 WGHN_AVRG_STCK  7 STCK_OPRC
//     8 STCK_HGPR       9 STCK_LWPR      10 ASKP1          11 BIDP1
//    12 CNTG_VOL       13 ACML_VOL       ...
@Component
public class KisTickFrameParser {

    public static final String TR_ID = "H0STCNT0";

    private static final int IDX_STOCK_CODE = 0;
    private static final int IDX_TRADE_TIME = 1;
    private static final int IDX_PRICE = 2;
    private static final int IDX_CHANGE = 4;
    private static final int IDX_CHANGE_RATE = 5;
    private static final int IDX_OPEN = 7;
    private static final int IDX_HIGH = 8;
    private static final int IDX_LOW = 9;
    private static final int IDX_TRADE_VOLUME = 12;
    private static final int IDX_CUMULATIVE_VOLUME = 13;
    private static final int MIN_FIELDS = IDX_CUMULATIVE_VOLUME + 1;

    public boolean isTickFrame(String frame) {
        if (frame == null || frame.isEmpty()) {
            return false;
        }
        int firstPipe = frame.indexOf('|');
        if (firstPipe < 0) {
            return false;
        }
        int secondPipe = frame.indexOf('|', firstPipe + 1);
        if (secondPipe < 0) {
            return false;
        }
        String trId = frame.substring(firstPipe + 1, secondPipe);
        return TR_ID.equals(trId);
    }

    public List<StockTick> parse(String frame) {
        List<StockTick> result = new ArrayList<>();
        if (!isTickFrame(frame)) {
            return result;
        }

        String[] header = frame.split("\\|", 4);
        if (header.length < 4) {
            return result;
        }
        String payload = header[3];

        String[] records = payload.split("\\+");
        for (String record : records) {
            if (record.isEmpty()) {
                continue;
            }
            String[] fields = record.split("\\^", -1);
            if (fields.length < MIN_FIELDS) {
                continue;
            }
            result.add(new StockTick(
                    fields[IDX_STOCK_CODE],
                    fields[IDX_TRADE_TIME],
                    parseDecimal(fields[IDX_PRICE]),
                    parseDecimal(fields[IDX_CHANGE]),
                    parseDecimal(fields[IDX_CHANGE_RATE]),
                    parseDecimal(fields[IDX_OPEN]),
                    parseDecimal(fields[IDX_HIGH]),
                    parseDecimal(fields[IDX_LOW]),
                    parseLong(fields[IDX_TRADE_VOLUME]),
                    parseLong(fields[IDX_CUMULATIVE_VOLUME])
            ));
        }
        return result;
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
