package org.kkobi.users.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.users.domain.UserVO;
import org.kkobi.users.dto.request.FriendRequestDto;
import org.kkobi.users.dto.response.FriendRequestResponseDto;
import org.kkobi.users.dto.response.FriendResponseDto;
import org.kkobi.users.enums.FriendshipStatus;
import org.kkobi.users.mapper.FriendMapper;
import org.kkobi.users.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService{

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;

    // 닉네임으로 친구 요청을 전송

    @Override
    @Transactional
    public void sendFriendRequest(Long userId, FriendRequestDto request) {

        UserVO targetUser = userMapper.findByNickname(request.getNickname());

        if(targetUser == null){
            throw new IllegalArgumentException("존재하지 않는 닉네임입니다.");
        }

        if(userId.equals(targetUser.getUserId())) {
            throw new IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }

        if(friendMapper.countRelationship(userId, targetUser.getUserId()) > 0){
            throw new IllegalArgumentException("이미 친구 관계 또는 친구 요청이 존재합니다.");
        }

        int insertRows = friendMapper.insertFriendRequest(
                userId,
                targetUser.getUserId(),
                FriendshipStatus.PENDING
        );

        if(insertRows != 1){
            throw new IllegalArgumentException("친구 요청 저장에 실패했습니다.");
        }
    }

    // 받은 친구 요청 목록을 조회
    @Override
    @Transactional(readOnly = true)
    public List<FriendRequestResponseDto> getReceiverFriendRequests(Long userId) {
        return friendMapper.findReceivedFriendRequests(userId);
    }

    // 받은 친구 요청을 수락
    @Override
    @Transactional
    public void acceptFriendRequest(Long userId, Long friendshipId) {

        int updateRows = friendMapper.acceptFriendRequest(
                friendshipId,
                userId
        );

        if (updateRows != 1) {
            throw new IllegalArgumentException("처리할 수 없는 친구 요청입니다.");
        }
    }

    // 받은 친구 요청을 거절
    @Override
    @Transactional
    public void rejectFriendRequest(Long userId, Long friendshipId) {

        int deletedRows = friendMapper.rejectFriendRequest(
                friendshipId,
                userId
        );

        if(deletedRows != 1){
            throw new IllegalArgumentException("처리할 수 없는 친구 요청입니다.");
        }
    }

    // 친구 목록을 조회
    @Override
    @Transactional(readOnly = true)
    public List<FriendResponseDto> getFriend(Long userId) {
        return friendMapper.findFriends(userId);
    }

    // 친구 관계를 삭제
    @Override
    @Transactional
    public void deleteFriend(Long userId, Long friendUserId) {

        int deletedRows = friendMapper.deleteFriend(
                userId,
                friendUserId
        );

        if (deletedRows != 1){
            throw new IllegalArgumentException("삭제할 수 없는 친구 관계입니다.");
        }
    }
}
