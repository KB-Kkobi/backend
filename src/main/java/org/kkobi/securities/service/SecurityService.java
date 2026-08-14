package org.kkobi.securities.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.service.AssessmentResultService;
import org.kkobi.exception.SecurityNotFoundException;
import org.kkobi.securities.dto.request.SecurityListRequest;
import org.kkobi.securities.dto.response.SecurityDetailResponse;
import org.kkobi.securities.dto.response.SecurityListItemResponse;
import org.kkobi.securities.dto.response.SecurityListResponse;
import org.kkobi.securities.dto.response.SecurityRecommendationResponse;
import org.kkobi.securities.enums.SecurityType;
import org.kkobi.securities.enums.StockSortType;
import org.kkobi.securities.mapper.SecurityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityService {

    // 페이지네이션 기본값
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final SecurityMapper securityMapper;
    private final AssessmentResultService assessmentResultService;

    // 종목 목록 조회
    @Transactional(readOnly = true)
    public SecurityListResponse getSecurityList(SecurityListRequest request, Long userId) {

        int page = normalizePage(request.getPage());
        int size = normalizeSize(request.getSize());
        int offset = (page - 1) * size;
        String keyword = request.getKeyword();

        // 사용자 성향 점수 조회 (없으면 null)
        boolean hasScore = assessmentResultService.existsAssessmentResult(userId);
        AssessmentScore score = hasScore ? assessmentResultService.getLatestAssessmentScore(userId) : null;

        // 정렬 파싱
        StockSortType sort = StockSortType.fromString(request.getSort());

        // 성향 결과 없이 match 정렬 요청 → volume으로 대체
        boolean sortFallback = false;
        if (sort == StockSortType.MATCH && !hasScore) {
            sort = StockSortType.VOLUME;
            sortFallback = true;
        }

        BigDecimal rtScore = score != null ? score.getRtScore() : null;
        BigDecimal lhScore = score != null ? score.getLhScore() : null;
        BigDecimal rpScore = score != null ? score.getRpScore() : null;

        List<SecurityListItemResponse> content = securityMapper.getSecurityList(
                request.getType(), keyword, offset, size, sort, rtScore, lhScore, rpScore);

        long totalElements = securityMapper.countSecurityList(request.getType(), keyword);
        int totalPages = calculateTotalPages(totalElements, size);

        SecurityListResponse response = new SecurityListResponse();
        response.setContent(content);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setSortFallback(sortFallback);
        response.setAppliedSort(sort.name().toLowerCase());

        return response;
    }

    // 홈 화면 추천 종목 조회 (주식 1 + 주식형 ETF 1 + 채권형 ETF 1, 성향 매칭 순)
    @Transactional(readOnly = true)
    public SecurityRecommendationResponse getRecommendedSecurities(Long userId) {

        // 사용자 성향 점수 조회 (없으면 null)
        boolean hasScore = assessmentResultService.existsAssessmentResult(userId);
        AssessmentScore score = hasScore ? assessmentResultService.getLatestAssessmentScore(userId) : null;

        // 성향 결과 없으면 match 대신 volume으로 대체
        StockSortType sort = hasScore ? StockSortType.MATCH : StockSortType.VOLUME;
        boolean sortFallback = !hasScore;

        BigDecimal rtScore = score != null ? score.getRtScore() : null;
        BigDecimal lhScore = score != null ? score.getLhScore() : null;
        BigDecimal rpScore = score != null ? score.getRpScore() : null;

        List<SecurityListItemResponse> content = new ArrayList<>();

        for (SecurityType type : List.of(
                SecurityType.STOCK, SecurityType.EQUITY_ETF, SecurityType.BOND_ETF)) {

            List<SecurityListItemResponse> topMatch = securityMapper.getSecurityList(
                    type, null, 0, 1, sort, rtScore, lhScore, rpScore);

            if (!topMatch.isEmpty()) {
                content.add(topMatch.get(0));
            }
        }

        SecurityRecommendationResponse response = new SecurityRecommendationResponse();
        response.setContent(content);
        response.setSortFallback(sortFallback);
        response.setAppliedSort(sort.name().toLowerCase());

        return response;
    }

    // ticker로 종목 상세 조회
    @Transactional(readOnly = true)
    public SecurityDetailResponse getSecurityByTicker(String ticker) {

        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker는 필수입니다.");
        }

        SecurityDetailResponse detail = securityMapper.getSecurityByTicker(ticker);

        if (detail == null) {
            throw new SecurityNotFoundException(
                    "존재하지 않는 종목입니다. ticker=" + ticker);
        }

        return detail;
    }

    // 페이지 번호를 정상 범위로 보정
    private int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    // 페이지당 조회 개수를 정상 범위로 보정
    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    // 전체 페이지 수 계산
    private int calculateTotalPages(long totalElements, int size) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }
}
