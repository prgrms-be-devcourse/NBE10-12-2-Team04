package com.triptrace.domain.member.member.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class MemberServiceTest {
    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원가입 시 회원이 ACTIVE 상태로 저장된다.")
    void signup() {
        Member member = memberService.signup("user@test.com", "user", "hashed", "imageUrl");

        Member found = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(found.getEmail()).isEqualTo("user@test.com");
        assertThat(found.getUsername()).isEqualTo("user");
        assertThat(found.getPasswordHash()).isEqualTo("hashed");
        assertThat(found.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("이미 사용중인 이메일이면 회원가입할 수 없다.")
    void signupDuplicateEmail() {
        memberService.signup("dup@test.com", "user1", "hashed", null);

        assertThatThrownBy(() -> memberService.signup("dup@test.com", "user2", "hashed", null))
            .isInstanceOf(ServiceException.class)
            .hasMessage("409-1 : 이미 사용중인 이메일입니다.");
    }

    @Test
    @DisplayName("이미 사용중인 닉네임이면 회원가입할 수 없다.")
    void signupDuplicateUsername() {
        memberService.signup("a@test.com", "dupname", "hashed", null);

        assertThatThrownBy(() -> memberService.signup("b@test.com", "dupname", "hashed", null))
            .isInstanceOf(ServiceException.class)
            .hasMessage("409-1 : 이미 사용중인 닉네임입니다.");
    }

    @Test
    @DisplayName("이메일로 회원을 조회할 수 있다.")
    void findByEmail() {
        memberService.signup("find@test.com", "finder", "hashed", null);

        Member found = memberService.findByEmail("find@test.com");

        assertThat(found.getUsername()).isEqualTo("finder");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 조회 시 401 예외가 발생한다.")
    void findByEmailNotFound() {
        assertThatThrownBy(() -> memberService.findByEmail("none@test.com"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("401-1 : 이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
