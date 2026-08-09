package org.kkobi.product.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.product.enums.PreferentialConditionType;
import org.kkobi.product.mapper.ProductMapper;
import org.kkobi.product.parser.PreferentialConditionParser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductPreferentialConditionService {

    private final ProductMapper productMapper;
    private final PreferentialConditionParser preferentialConditionParser;

    // 상품의 우대조건을 현재 원문 기준으로 다시 저장
    public void replacePreferentialConditions(
            Long productId,
            String preferentialConditions
    ) {
        // 기존 우대조건 삭제
        productMapper.deleteProductPreferentialConditions(productId);

        // 우대조권 원문을 유형별로 파싱
        List<PreferentialConditionType> conditionTypes =
                preferentialConditionParser.parse(preferentialConditions);

        // 파싱된 우대조건 유형 저장
        for (PreferentialConditionType conditionType : conditionTypes){
            productMapper.saveProductPreferentialCondition(
                    productId,
                    conditionType
            );
        }
    }
}
