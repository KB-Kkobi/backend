package org.kkobi.goal.enums;

public enum FinancialGoalType {
    LUMP_SUM("목돈 마련"),
    TRAVEL("여행 자금"),
    ELECTRONICS("전자기기 구매"),
    EDUCATION("교육 자금"),
    HOUSING("주거 자금"),
    EMERGENCY("비상금"),
    OTHER("기타");

    private final String label;

    FinancialGoalType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
