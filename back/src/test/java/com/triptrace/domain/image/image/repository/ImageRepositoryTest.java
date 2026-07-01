package com.triptrace.domain.image.image.repository;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.repository.TripLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ImageRepositoryTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private TripLikeRepository tripLikeRepository;

    @Test
    @DisplayName("게시글 ID로 이미지 목록을 조회한다")
    void findByPostId() {
        Member member = memberRepository.save(new Member(
            "user@example.com",
            "traveler",
            "passwordHash",
            "api-key",
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
        Post post = postRepository.save(new Post(
            trip,
            LocalDate.of(2024, 4, 1),
            "첫날, 교토 도착",
            "교토에 도착했다."
        ));
        Post anotherPost = postRepository.save(new Post(
            trip,
            LocalDate.of(2024, 4, 1),
            "저녁 기록",
            "저녁을 먹었다."
        ));
        Image image = imageRepository.save(new Image(
            member,
            trip,
            post,
            "https://example.com/images/kyoto.jpg",
            null,
            1024L,
            "image/jpeg",
            UploadStatus.STORED
        ));
        imageRepository.save(new Image(
            member,
            trip,
            anotherPost,
            "https://example.com/images/dinner.jpg",
            null,
            2048L,
            "image/jpeg",
            UploadStatus.STORED
        ));
    }
}
