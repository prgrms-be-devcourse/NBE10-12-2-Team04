package com.triptrace.domain.marker.marker.entity;

import com.triptrace.domain.image.image.entity.Image;
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

    @Column(precision = 10, scale = 7)    // 정수 3자리, 소수 7자리
    private BigDecimal centerLat;

    @Column(precision = 10, scale = 7)
    private BigDecimal centerLng;

    @Column(length = 100)
    private String placeName;

    private LocalDateTime visitedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private MarkerSource source;    //자동생성: AUTO, 수동 생성: MANUAL

    // 마커 카드/지도 표시에서 사용할 대표 이미지. 수동 마커는 이미지 없이 생성될 수 있어 null을 허용한다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_image_id")
    private Image representativeImage;

    public Marker(Post post, BigDecimal centerLat, BigDecimal centerLng, String placeName, LocalDateTime visitedAt, MarkerSource source) {
        this(post, centerLat, centerLng, placeName, visitedAt, source, null);
    }  // 수동 마커의 경우 대표이미지 없이 들어올 수 있어 생성자 오버로딩 처리.

    public Marker(
        Post post,
        BigDecimal centerLat,
        BigDecimal centerLng,
        String placeName,
        LocalDateTime visitedAt,
        MarkerSource source,
        Image representativeImage
    ) {
        this.post = post;
        this.centerLat = centerLat;
        this.centerLng = centerLng;
        this.placeName = placeName;
        this.visitedAt = visitedAt;
        this.source = source;
        this.representativeImage = representativeImage;
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

    // 자동 생성 이후 대표 이미지를 재선택하거나, 수동 편집에서 대표 이미지를 교체할 때 사용한다.
    // 변경 이유가 달라 modify와 분리
    public void changeRepresentativeImage(Image representativeImage) {
        this.representativeImage = representativeImage;
    }
}
