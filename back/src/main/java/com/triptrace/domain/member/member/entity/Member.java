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

    public void modifyProfile(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
