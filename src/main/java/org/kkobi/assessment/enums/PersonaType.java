package org.kkobi.assessment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PersonaType {
    FLAME_CHASER("불꽃 추격자"),
    SMART_TRADER("스마트 단타러"),
    AMBITIOUS_PIONEER("야망찬 개척자"),
    CONVICTION_VALUE_INVESTOR("신념의 가치투자자"),
    PRACTICAL_INFORMATION_SEEKER("실속파 정보통"),
    CASH_PRESERVER("현금 확보주의자"),
    STEADY_ACCUMULATOR("묵묵한 적립왕"),
    STRICT_VAULT_KEEPER("철저한 금고지기");

    private final String personaName;
}
