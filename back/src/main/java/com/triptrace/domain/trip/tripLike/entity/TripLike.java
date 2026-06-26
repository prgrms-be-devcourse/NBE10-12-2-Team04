package com.triptrace.domain.trip.tripLike.entity;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip_like",
    // 복합 유니크 설정. 같은 사용자가 같은 여행기에 좋아요를 중복x
    uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "trip_id"}))
@Getter
@NoArgsConstructor
public class TripLike extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    public TripLike(Member member, Trip trip) {
        this.member = member;
        this.trip = trip;
    }
}
