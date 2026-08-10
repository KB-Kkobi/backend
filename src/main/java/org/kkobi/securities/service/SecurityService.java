package org.kkobi.securities.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.exception.SecurityNotFoundException;
import org.kkobi.securities.dto.request.SecurityListRequest;
import org.kkobi.securities.dto.response.SecurityDetailResponse;
import org.kkobi.securities.dto.response.SecurityListItemResponse;
import org.kkobi.securities.dto.response.SecurityListResponse;
import org.kkobi.securities.mapper.SecurityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityService {

    // 페이지네이션 기본값
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final SecurityMapper securityMapper;

    // 종목 목록 조회
    @Transactional(readOnly = true)
    public SecurityListResponse getSecurityList(SecurityListRequest request) {

        int page = normalizePage(request.getPage());
        int size = normalizeSize(request.getSize());
        int offset = (page - 1) * size;

        String keyword = request.getKeyword();

        List<SecurityListItemResponse> content =
                securityMapper.getSecurityList(request.getType(), keyword, offset, size);

        long totalElements = securityMapper.countSecurityList(request.getType(), keyword);
        int totalPages = calculateTotalPages(totalElements, size);

        SecurityListResponse response = new SecurityListResponse();
        response.setContent(content);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);

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
