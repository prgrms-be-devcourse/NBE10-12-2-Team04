package com.triptrace.domain.auth.auth.service;

import com.triptrace.domain.auth.auth.entity.RefreshToken;
import com.triptrace.domain.auth.auth.repository.RefreshTokenRepository;
import com.triptrace.domain.member.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리프레시 토큰의 "별도 트랜잭션"이 필요한 처리를 담당한다.
 * AuthService(재발급) 내부 호출로는 REQUIRES_NEW가 동작하지 않으므로 별도 빈으로 분리했다.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 탈취 감지 시 해당 회원의 모든 RT를 무효화한다.
     * 재발급 트랜잭션이 예외로 롤백되더라도 이 무효화는 남아야 하므로 REQUIRES_NEW로 독립 커밋한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllByMember(Member member) {
        refreshTokenRepository.findAllByMember(member)
            .forEach(RefreshToken::revoke);
    }
}
