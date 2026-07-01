package com.triptrace.domain.auth.auth.repository;

import com.triptrace.domain.auth.auth.entity.RefreshToken;
import com.triptrace.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByMember(Member member);

    void deleteAllByMember(Member member);
}
