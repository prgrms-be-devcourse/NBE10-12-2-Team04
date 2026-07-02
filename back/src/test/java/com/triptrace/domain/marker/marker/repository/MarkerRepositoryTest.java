package com.triptrace.domain.marker.marker.repository;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.entity.MarkerSource;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class MarkerRepositoryTest {

    @Autowired
    private MarkerRepository markerRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Test
    @DisplayName("게시글 ID로 대표 마커를 조회한다")
    void findByPostId() {
        Post post = savePost("첫날, 교토 도착");
        Marker marker = markerRepository.save(new Marker(
            post,
            BigDecimal.valueOf(35.0116363),
            BigDecimal.valueOf(135.7680294),
            "교토역",
            LocalDateTime.of(2024, 4, 1, 12, 0),
            MarkerSource.AUTO
        ));

        Optional<Marker> foundMarker = markerRepository.findByPostId(post.getId());

        assertThat(foundMarker).contains(marker);
        assertThat(foundMarker.get().getRepresentativeImage()).isNull();
    }

    @Test
    @DisplayName("하나의 게시글에는 마커를 하나만 연결할 수 있다")
    void uniquePost() {
        Post post = savePost("첫날, 교토 도착");
        markerRepository.saveAndFlush(new Marker(
            post,
            BigDecimal.valueOf(35.0116363),
            BigDecimal.valueOf(135.7680294),
            "교토역",
            LocalDateTime.of(2024, 4, 1, 12, 0),
            MarkerSource.AUTO
        ));

        Marker duplicateMarker = new Marker(
            post,
            BigDecimal.valueOf(35.003815),
            BigDecimal.valueOf(135.77859),
            "기온",
            LocalDateTime.of(2024, 4, 1, 15, 0),
            MarkerSource.MANUAL
        );

        assertThatThrownBy(() -> markerRepository.saveAndFlush(duplicateMarker))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("마커는 대표 이미지를 연결할 수 있다")
    void representativeImage() {
        Post post = savePost("대표 이미지가 있는 마커");
        Image image = imageRepository.save(new Image(
            post.getTrip().getOwner(),
            post.getTrip(),
            post,
            "https://example.com/images/kyoto.jpg",
            "https://example.com/images/kyoto-thumbnail.jpg",
            1024L,
            "image/jpeg",
            UploadStatus.STORED
        ));

        Marker marker = markerRepository.saveAndFlush(new Marker(
            post,
            BigDecimal.valueOf(35.0116363),
            BigDecimal.valueOf(135.7680294),
            "교토역",
            LocalDateTime.of(2024, 4, 1, 12, 0),
            MarkerSource.AUTO,
            image
        ));

        Optional<Marker> foundMarker = markerRepository.findByPostId(post.getId());

        assertThat(foundMarker).contains(marker);
        assertThat(foundMarker.get().getRepresentativeImage()).isEqualTo(image);
        // 마커는 대표 Image를 참조하고, 화면용 썸네일 URL은 Image에서 가져온다.
        assertThat(foundMarker.get().getRepresentativeImage().getThumbnailUrl())
            .isEqualTo("https://example.com/images/kyoto-thumbnail.jpg");
    }

    private Post savePost(String title) {
        Member member = memberRepository.save(new Member(
            title + "@example.com",
            title,
            "passwordHash",
            null,
            MemberStatus.ACTIVE
        ));
        Trip trip = tripRepository.save(new Trip(
            member,
            "교토 여행",
            "일본",
            "교토",
            LocalDateTime.of(2024, 4, 1, 0, 0),
            LocalDateTime.of(2024, 4, 5, 0, 0),
            true
        ));

        return postRepository.save(new Post(
            trip,
            LocalDate.of(2024, 4, 1),
            title,
            "교토에 도착했다."
        ));
    }
}
