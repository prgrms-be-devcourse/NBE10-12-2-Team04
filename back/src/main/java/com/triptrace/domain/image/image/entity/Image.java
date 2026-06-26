package com.triptrace.domain.image.image.entity;

import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Image extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marker_id")
    private Marker marker;

    @Column(nullable = false)
    private String originalFileUrl;

    private String thumbnailUrl;

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 50, nullable = false)
    private String mimeType;

    // EXIF 분석 완료 후 채워지는 이미지 메타데이터
    @Column(precision = 10, scale = 7) // 정수 3자리, 소수 7자리
    private BigDecimal gpsLat;

    @Column(precision = 10, scale = 7)
    private BigDecimal gpsLng;

    private LocalDateTime capturedAt;

    private String deviceInfo;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private UploadStatus uploadStatus;

    // 업로드 시점에 알 수 있는 기본 이미지 정보만 먼저 저장
    public Image(
        Member owner,
        Trip trip,
        Marker marker,
        String originalFileUrl,
        String thumbnailUrl,
        Long fileSize,
        String mimeType,
        UploadStatus uploadStatus
    ) {
        this.owner = owner;
        this.trip = trip;
        this.marker = marker;
        this.originalFileUrl = originalFileUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.uploadStatus = uploadStatus;
    }
}
