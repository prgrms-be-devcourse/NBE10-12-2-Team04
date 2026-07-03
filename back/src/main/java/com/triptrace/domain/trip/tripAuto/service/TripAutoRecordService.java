package com.triptrace.domain.trip.tripAuto.service;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.image.image.support.ImageUrlResolver;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.entity.MarkerSource;
import com.triptrace.domain.marker.marker.geocoding.ReverseGeocodingClient;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripAuto.dto.TripAutoRecordResponse;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
public class TripAutoRecordService {

    // 같은 날짜 안에서도 클러스터 첫 사진과 2시간을 넘게 차이나면 다른 묶음으로 분리
    private static final long CLUSTER_TIME_GAP_MINUTES = 120;

    private final TripRepository tripRepository;
    private final ImageRepository imageRepository;
    private final PostRepository postRepository;
    private final MarkerRepository markerRepository;
    private final ReverseGeocodingClient reverseGeocodingClient;
    private final ImageUrlResolver imageUrlResolver;

    // 자동 생성은 Post 생성, Marker 생성, Image 연결이 하나의 작업이므로 중간 실패 시 전체 롤백
    @Transactional
    public TripAutoRecordResponse createAutoRecords(Long tripId, Long ownerId) {
        // 자동 생성 대상 여행기를 조회하고, 요청자가 해당 여행기의 소유자인지 확인
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ServiceException("404-1", "여행기를 찾을 수 없습니다."));

        validateOwner(trip, ownerId);

        // 업로드 단계에서 저장된 Image row만 조회
        List<Image> images = imageRepository.findByTripId(tripId);

        // 자동 생성에는 촬영 시간과 GPS가 모두 있는 이미지만 사용
        // 메타데이터가 부족한 이미지는 실패가 아니라 제외 대상으로 보고 응답 count에 포함
        List<Image> usableImages = images.stream()
            .filter(this::hasAutoRecordMetadata)
            .sorted(Comparator.comparing(Image::getCapturedAt))
            .toList();

        // 날짜를 먼저 나눈 뒤, 같은 날짜 안에서 촬영 시간 간격을 기준으로 세부 클러스터를 만든다.
        List<List<Image>> clusters = clusterImages(usableImages);
        List<TripAutoRecordResponse.GeneratedRecord> records = new ArrayList<>();

        for (List<Image> cluster : clusters) {
            // 현재는 클러스터의 첫 번째 이미지를 대표 이미지로 사용
            // 대표 이미지의 촬영 시간과 GPS가 Post/Marker 생성 기준이 된다.
            Image representativeImage = selectRepresentativeImage(cluster);
            LocalDate recordDate = representativeImage.getCapturedAt().toLocalDate();

            // 장소명은 대표 이미지 GPS를 역지오코딩해서 채운다. 실패하면 기본 문구로 두고 생성은 계속 진행한다.
            String placeName = reverseGeocodingClient.findPlaceName(
                representativeImage.getGpsLat(),
                representativeImage.getGpsLng()
            );
            String postTitle = "%s 근처".formatted(StringUtils.hasText(placeName) ? placeName : "위치 미정");

            // 클러스터 하나를 여행 기록 게시물 하나로 변환
            Post post = postRepository.save(new Post(
                trip,
                recordDate,
                postTitle,
                ""
            ));

            // 클러스터 하나를 지도 마커 하나로 변환
            // Marker는 대표 Image만 참조
            Marker marker = markerRepository.save(new Marker(
                post,
                truncateCoordinate(representativeImage.getGpsLat()),
                truncateCoordinate(representativeImage.getGpsLng()),
                placeName,
                representativeImage.getCapturedAt(),
                MarkerSource.AUTO,
                representativeImage
            ));

            if (trip.getRepresentativeImage() == null) {
                trip.changeRepresentativeImage(representativeImage);
            }

            // 자동 생성된 Post에 클러스터 내 이미지들을 연결
            // 트랜잭션 안의 영속 엔티티라 별도 save 없음
            cluster.forEach(image -> image.connectPost(post));

            // 클라이언트가 생성 결과를 바로 확인하거나 조회할 수 있도록 id 목록을 응답에 담는다.
            records.add(new TripAutoRecordResponse.GeneratedRecord(
                post.getId(),
                marker.getId(),
                representativeImage.getId(),
                imageUrlResolver.toPublicUrl(representativeImage.getThumbnailUrl()),
                postTitle,
                placeName,
                recordDate,
                marker.getCenterLat(),
                marker.getCenterLng(),
                cluster.stream()
                    .map(Image::getId)
                    .toList()
            ));
        }

        return new TripAutoRecordResponse(
            trip.getId(),
            records.size(), //생성된 post카운트
            records.size(), //생성된 marker카운트 -> 자동생성이라 post와 marker의 개수가 같음
            usableImages.size(),
            images.size() - usableImages.size(),
            records
        );
    }

    private List<List<Image>> clusterImages(List<Image> images) {
        // 먼저 날짜별로 큰 묶음을 만든다. 서로 다른 날짜의 사진은 같은 기록으로 묶지 않는다.
        Map<LocalDate, List<Image>> imagesByDate = images.stream()
            .collect(groupingBy(
                image -> image.getCapturedAt().toLocalDate(),
                TreeMap::new,
                java.util.stream.Collectors.toList()
            )); // 날짜 순서대로 처리되도록 TreeMap에 저장

        List<List<Image>> clusters = new ArrayList<>();

        for (List<Image> dailyImages : imagesByDate.values()) {
            // 같은 날짜 안에서는 촬영 시간순으로 돌면서 클러스터 첫 이미지와 비교해 새 묶음 여부를 결정
            List<Image> currentCluster = new ArrayList<>();
            Image clusterStartImage = null;

            for (Image image : dailyImages) {
                // 클러스터 첫 사진 기준 2시간을 넘으면 현재 묶음을 닫고 새 묶음을 시작
                if (clusterStartImage != null && shouldStartNewCluster(clusterStartImage, image)) {
                    clusters.add(currentCluster);
                    currentCluster = new ArrayList<>();
                    clusterStartImage = image;
                }

                if (clusterStartImage == null) {
                    clusterStartImage = image;
                }

                currentCluster.add(image);
            }

            if (!currentCluster.isEmpty()) {
                clusters.add(currentCluster);
            }
        }

        return clusters;
    }

    private boolean shouldStartNewCluster(Image clusterStartImage, Image currentImage) {
        // 같은 날짜 안에서 클러스터 첫 사진과 2시간을 초과해 떨어진 사진은 다른 기록으로 판단
        return exceedsTimeGap(clusterStartImage, currentImage);
    }

    private boolean exceedsTimeGap(Image clusterStartImage, Image currentImage) {
        // 클러스터가 계속 이어져도 시작 사진 기준 2시간을 넘으면 별도 기록으로 나눔
        long minutes = Duration.between(
            clusterStartImage.getCapturedAt(),
            currentImage.getCapturedAt()
        ).abs().toMinutes();

        return minutes > CLUSTER_TIME_GAP_MINUTES;
    }

    private Image selectRepresentativeImage(List<Image> images) {
        // 1차 구현에서는 시간순 첫 번째 이미지를 대표 이미지로 둔다.
        // 이후에는 썸네일 품질, 장소 정확도, 사용자 선택값 등을 기준에 추가할 수 있다.
        return images.getFirst();
    }

    private BigDecimal truncateCoordinate(BigDecimal coordinate) {
        // Marker 중심 좌표는 화면 표시/검색 기준으로 소수점 4자리까지만 남기고 버림.
        return coordinate.setScale(4, RoundingMode.DOWN);
    }

    private void validateOwner(Trip trip, Long ownerId) {
        if (!trip.getOwner().getId().equals(ownerId)) {
            throw new ServiceException("403-1", "권한이 없습니다.");
        }
    }

    private boolean hasAutoRecordMetadata(Image image) {
        // 촬영 시간과 GPS가 모두 있어야 자동 분리 기준으로 사용 가능
        return image.getCapturedAt() != null
            && image.getGpsLat() != null
            && image.getGpsLng() != null;
    }
}
