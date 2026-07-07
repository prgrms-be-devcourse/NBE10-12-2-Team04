package com.triptrace.domain.member.member.entity;

import com.triptrace.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @Column(length = 255, nullable = false)
    private String passwordHash;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 100)
    private String intro;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    private LocalDateTime deletedAt;

    public Member(
        String email,
        String username,
        String passwordHash,
        String profileImageUrl,
        MemberStatus status
    ) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.profileImageUrl = profileImageUrl;
        this.status = status;
    }

    // 부분 수정: null 인 값은 "변경하지 않음"으로 보고, 넘어온 값만 반영한다.
    public void modifyInfo(String username, String intro, String profileImageUrl) {
        if (username != null) {
            this.username = username;
        }
        if (intro != null) {
            this.intro = intro;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }
}
