package org.kkobi.product.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.product.dto.response.ProductOptionResponseDto;
import org.kkobi.product.enums.PreferentialConditionType;
import org.kkobi.product.mapper.ProductMapper;
import org.kkobi.product.parser.PreferentialConditionParser;
import org.kkobi.product.parser.PreferentialRateConditionParser;
import org.kkobi.product.parser.dto.ParsedPreferentialRateCondition;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductPreferentialConditionService {

    private final ProductMapper productMapper;
    private final PreferentialConditionParser preferentialConditionParser;
    private final PreferentialRateConditionParser preferentialRateConditionParser;

    // 상품의 우대조건을 현재 원문 기준으로 다시 저장
    public void replacePreferentialConditions(
            Long productId,
            String preferentialConditions
    ) {
        replaceFilterConditions(
                productId,
                preferentialConditions
        );

        replaceRateConditions(
                productId,
                preferentialConditions
        );
    }

    // 상품 목록 필터에서 사용할 우대조건 유형 저장
    private void replaceFilterConditions(
            Long productId,
            String preferentialConditions
    ) {
        // 기존 필터용 우대조건 삭제
        productMapper.deleteProductPreferentialConditions(
                productId
        );

        // 우대조건 원문을 유형별로 파싱
        List<PreferentialConditionType> conditionTypes =
                preferentialConditionParser.parse(
                        preferentialConditions
                );

        // 파싱된 우대조건 유형 저장
        for (PreferentialConditionType conditionType
                : conditionTypes) {

            productMapper.saveProductPreferentialCondition(
                    productId,
                    conditionType
            );
        }
    }

    // 상품 옵션별 추가 우대금리 조건 저장
    private void replaceRateConditions(
            Long productId,
            String preferentialConditions
    ) {
        // 기존 옵션별 우대금리 조건 삭제
        productMapper.deleteProductPreferentialRateConditions(
                productId
        );

        // 상품에 등록된 모든 금리 옵션 조회
        List<ProductOptionResponseDto> options =
                productMapper.getProductOptions(
                        productId
                );

        for (ProductOptionResponseDto option : options) {
            List<ParsedPreferentialRateCondition> conditions =
                    preferentialRateConditionParser.parse(
                            preferentialConditions,
                            option.getSavingTerm()
                    );

           Set<String> savedConditionKeys = new HashSet<>();

           int displayOrder = 1;

           for(ParsedPreferentialRateCondition condition : conditions){
               String conditionKey =
                       condition.getConditionType()
                               + "|"
                               + condition.getConditionName()
                               + "|"
                               + condition.getAdditionalRate()
                               + "|"
                               + condition.isSelectable();

               if (!savedConditionKeys.add(conditionKey)) {
                   continue;
               }

               productMapper.saveProductPreferentialRateCondition(
                       option.getProductOptionId(),
                       condition.getConditionType(),
                       condition.getConditionName(),
                       condition.getAdditionalRate(),
                       condition.isSelectable(),
                       displayOrder
               );

               displayOrder++;
            }
        }
    }
}