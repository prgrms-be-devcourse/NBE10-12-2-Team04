package com.triptrace.domain.post.post.entity;

import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Post extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 100, nullable = false)
    private String title;

    @Lob // TEXT로 매핑
    private String memo;

    public Post(Trip trip, LocalDate date, String title, String memo) {
        this.trip = trip;
        this.date = date;
        this.title = title;
        this.memo = memo;
    }
}
