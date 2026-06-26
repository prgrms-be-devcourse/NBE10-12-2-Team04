package com.triptrace.domain.marker.marker.entity;

import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Marker extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @Column(precision = 10, scale = 7, nullable = false)    // 정수 3자리, 소수 7자리
    private BigDecimal centerLat;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal centerLng;

    @Column(length = 100)
    private String placeName;

    private LocalDateTime visitedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private MarkerSource source;    //자동생성: AUTO, 수동 생성: MANUAL

    public Marker(Post post, BigDecimal centerLat, BigDecimal centerLng, String placeName, LocalDateTime visitedAt, MarkerSource source) {
        this.post = post;
        this.centerLat = centerLat;
        this.centerLng = centerLng;
        this.placeName = placeName;
        this.visitedAt = visitedAt;
        this.source = source;
    }
    public void modify(
        BigDecimal centerLat,
        BigDecimal centerLng,
        String placeName,
        LocalDateTime visitedAt,
        MarkerSource source
    ) {
        this.centerLat = centerLat;
        this.centerLng = centerLng;
        this.placeName = placeName;
        this.visitedAt = visitedAt;
        this.source = source;
    }
}
