package com.triptrace.domain.trip.trip.entity;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Trip extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false)
    private String title;

    private String country;

    private String city;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Column(nullable = false)
    private boolean visibility;

    @Column(nullable = false)
    private Long likeCount = 0L;

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        this.likeCount--;
    }

    public Trip(
        Member owner,
        String title,
        String country,
        String city,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean visibility
    ) {
        this.owner = owner;
        this.title = title;
        this.country = country;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.visibility = visibility;
    }
}
