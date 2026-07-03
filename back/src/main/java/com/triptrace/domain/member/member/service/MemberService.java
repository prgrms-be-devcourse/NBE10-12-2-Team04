package com.triptrace.domain.member.member.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.global.exception.ServiceException;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 데이터의 규칙과 영속화를 담당한다. (중복 검사 → 저장, 이메일로 조회)
 * 비밀번호 해시·토큰 발급 같은 인증 조립은 AuthService의 몫이며, 여기선 받은 값을 다룰 뿐이다.
 */
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    // 회원가입: 이메일/닉네임 중복을 막고 새 회원을 저장한다. (passwordHash는 이미 해시된 값)
    @Transactional
    public Member signup(String email, String username, String passwordHash, String profileImageUrl) {
        if (memberRepository.existsByEmail(email)) {
            throw new ServiceException("409-1", "이미 사용중인 이메일입니다.");
        }

        if (memberRepository.existsByUsername(username)) {
            throw new ServiceException("409-1", "이미 사용중인 닉네임입니다.");
        }

        Member member = new Member(email, username, passwordHash, profileImageUrl, MemberStatus.ACTIVE);

        return memberRepository.save(member);
    }

    // 로그인용 조회: 없으면 이메일 존재 여부를 노출하지 않도록 비밀번호 오류와 동일한 메시지로 처리한다.
    @Transactional(readOnly = true)
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new ServiceException("401-1", "이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Transactional(readOnly = true)
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));
    }

    @Transactional
    public void modifyProfileImageUrl(Long id, String profileImageUrl) {
        Member member = findById(id);
        member.modifyProfile(profileImageUrl);
        memberRepository.save(member);
    }
}
