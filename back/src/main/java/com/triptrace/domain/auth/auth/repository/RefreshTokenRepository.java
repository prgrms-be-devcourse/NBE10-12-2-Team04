package com.triptrace.domain.auth.auth.repository;

import com.triptrace.domain.auth.auth.entity.RefreshToken;
import com.triptrace.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
// 이부분은 추후 구현 후 어디에 써야하는지 명시적으로 알려주자.

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByMember(Member member);

    void deleteAllByMember(Member member);
}
