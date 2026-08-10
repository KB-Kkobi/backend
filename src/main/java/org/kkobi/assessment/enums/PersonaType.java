package org.kkobi.assessment.enums;

public enum PersonaType {
    HHH,
    HHL,
    HLH,
    HLL,
    LHH,
    LHL,
    LLH,
    LLL;

    public String getAxisCode() {
        return name();
    }
}
