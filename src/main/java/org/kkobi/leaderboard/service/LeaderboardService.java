package org.kkobi.leaderboard.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.leaderboard.dto.LeaderboardCacheDto;
import org.kkobi.leaderboard.dto.LeaderboardItemResponseDto;
import org.kkobi.leaderboard.dto.LeaderboardResponseDto;
import org.kkobi.users.dto.response.FriendResponseDto;
import org.kkobi.users.service.FriendService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardRedisService leaderboardRedisService;

    private final FriendService friendService;

    // 로그인 사용자의 투자 성향 리더보드를 조회
    public LeaderboardResponseDto getPersonaLeaderboard(Long userId) {

        LeaderboardCacheDto myCache =
                leaderboardRedisService.getUserCache(userId);

        if(myCache == null || myCache.getPersonaId() == null){
            return createEmptyResponse();
        }

        Long personaId = myCache.getPersonaId();

        List<Long> rankingUserIds =
        leaderboardRedisService
                .getPersonaRankingUserIds(personaId);

        List<LeaderboardCacheDto> rankingCaches =
                leaderboardRedisService
                        .getUserCaches(rankingUserIds);

        List<LeaderboardItemResponseDto> rankings =
                createRankingItems(
                        rankingUserIds,
                        rankingCaches
                );

        Long myRank =
                leaderboardRedisService
                        .getPersonaRank(
                                personaId,
                                userId
                        );

        LeaderboardResponseDto response = new LeaderboardResponseDto();

        response.setMyRank(myRank);
        response.setPersonaId(personaId);
        response.setPersonaName(myCache.getPersonaName());
        response.setRankings(rankings);

        return response;
    }

    // Redis 데이터를 리더보드 응답 목록으로 변환
    private List<LeaderboardItemResponseDto> createRankingItems(
            List<Long> rankingUserIds,
            List<LeaderboardCacheDto> rankingCaches
    ) {
        if (rankingUserIds.isEmpty() || rankingCaches.isEmpty()) {
            return Collections.emptyList();
        }

        List<LeaderboardItemResponseDto> rankings = new ArrayList<>();

        for(int i = 0; i < rankingUserIds.size(); i++){
            Long userId = rankingUserIds.get(i);

            LeaderboardCacheDto cacheDto =
                    findUserCache(
                            rankingCaches,
                            userId
                    );

            if(cacheDto == null){
                continue;
            }

            LeaderboardItemResponseDto item = new LeaderboardItemResponseDto();

            item.setRank((long) i + 1);
            item.setUserId(cacheDto.getUserId());
            item.setNickname(cacheDto.getNickname());
            item.setPersonaId(cacheDto.getPersonaId());
            item.setPersonaName(cacheDto.getPersonaName());
            item.setTotalAsset(cacheDto.getTotalAsset());
            item.setReturnRate(cacheDto.getReturnRate());

            rankings.add(item);
        }
        return rankings;
    }

    // 사용자 ID에 해당하는 Redis 캐시를 조회
    private LeaderboardCacheDto findUserCache(
            List<LeaderboardCacheDto> caches,
            Long userId
    ) {
        for(LeaderboardCacheDto cache : caches){
            if(userId.equals(cache.getUserId())){
                return cache;
            }
        }

        return null;
    }

    // 조회할 리더보드가 없는 경우 빈 응답을 생성
    private LeaderboardResponseDto createEmptyResponse() {

        LeaderboardResponseDto response = new LeaderboardResponseDto();

        response.setMyRank(null);
        response.setPersonaId(null);
        response.setPersonaName(null);
        response.setRankings(Collections.emptyList());

        return response;
    }

    // 로그인 사용자와 친구들의 리더보드를 조회
    public LeaderboardResponseDto getFriendLeaderboard(Long userId){

        List<FriendResponseDto> friends = friendService.getFriend(userId);

        List<Long> userIds = new ArrayList<>();

        // 로그인 사용자도 친구 리더보드에 포함
        userIds.add(userId);

        for(FriendResponseDto friend : friends) {
            userIds.add(friend.getUserId());
        }

        List<LeaderboardCacheDto> caches = leaderboardRedisService.getUserCaches(userIds);

        caches.sort(
                Comparator.comparing(
                        LeaderboardCacheDto::getReturnRate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        List<LeaderboardItemResponseDto> rankings = createFriendsRankingItems(caches);

        Long myRank = findMyRank(rankings, userId);

        LeaderboardResponseDto response = new LeaderboardResponseDto();

        response.setMyRank(myRank);
        response.setPersonaId(null);
        response.setPersonaName(null);
        response.setRankings(rankings);

        return response;
    }

    // 친구 리더보드 응랍 목록을 생성
    private List<LeaderboardItemResponseDto> createFriendsRankingItems(
            List<LeaderboardCacheDto> caches
    ) {
        List<LeaderboardItemResponseDto> rankings = new ArrayList<>();

        for(int i = 0; i < caches.size(); i++) {
            LeaderboardCacheDto cacheDto = caches.get(i);

            LeaderboardItemResponseDto item = new LeaderboardItemResponseDto();

            item.setRank((long) i + 1);
            item.setUserId(cacheDto.getUserId());
            item.setNickname(cacheDto.getNickname());
            item.setPersonaId(cacheDto.getPersonaId());
            item.setPersonaName(cacheDto.getPersonaName());
            item.setTotalAsset(cacheDto.getTotalAsset());
            item.setReturnRate(cacheDto.getReturnRate());

            rankings.add(item);
        }

        return rankings;
    }

    // 친구 리더보드에서 로그인 사용자의 순위를 조회
    private Long findMyRank(
            List<LeaderboardItemResponseDto> rankings,
            Long userId
    ){
        for(LeaderboardItemResponseDto item : rankings){
            if(userId.equals(item.getUserId())) {
                return item.getRank();
            }
        }

        return null;
    }
}
