package com.triptrace.domain.image.image.service;

import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ImageServiceImplTest {
    @Autowired
    private ImageService imageService;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private PostRepository postRepository;

    private Member createMember(String username) {
        return memberRepository.save(new Member(
            "%s@test.com".formatted(username), username, "passwordHash", null, MemberStatus.ACTIVE
        ));
    }

    private Trip createTrip(Member owner) {
        return tripRepository.save(new Trip(
            owner, "교토 여행", "일본", "교토",
            LocalDateTime.of(2024, 4, 1, 0, 0),
            LocalDateTime.of(2024, 4, 5, 0, 0),
            true
        ));
    }

    private Post createPost(Trip trip) {
        return postRepository.save(new Post(
            trip, LocalDate.of(2024, 4, 1), "첫날", "교토에 도착했다."
        ));
    }

    private Image createImage(Member owner, Trip trip, Post post) {
        return imageRepository.save(new Image(
            owner, trip, post,
            "https://example.com/images/%s.jpg".formatted(java.util.UUID.randomUUID()),
            null, 1024L, "image/jpeg", UploadStatus.STORED
        ));
    }

    @Test
    @DisplayName("post 없이 업로드된 이미지에 post를 지정할 수 있다")
    void modifyPost() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = createImage(owner, trip, null);

        ImageServiceResponse response = imageService.modifyPost(owner, trip, post, image.getId());

        assertThat(response.post().getId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("소유자가 아니면 post 지정 시 예외가 발생한다")
    void modifyPostNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = createImage(owner, trip, null);

        assertThatThrownBy(() -> imageService.modifyPost(other, trip, post, image.getId()))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("이미지가 속하지 않은 trip으로 post 지정 시 예외가 발생한다")
    void modifyPostWrongTrip() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Trip otherTrip = createTrip(owner);
        Post postInOtherTrip = createPost(otherTrip);
        Image image = createImage(owner, trip, null);

        assertThatThrownBy(() -> imageService.modifyPost(owner, otherTrip, postInOtherTrip, image.getId()))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("id로 이미지를 삭제하면 더 이상 조회되지 않는다")
    void deleteById() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = createImage(owner, trip, post);

        imageService.delete(owner, trip, post, image.getId());

        assertThat(imageRepository.findById(image.getId())).isEmpty();
    }

    @Test
    @DisplayName("url로 이미지를 삭제하면 더 이상 조회되지 않는다")
    void deleteByUrl() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = createImage(owner, trip, post);

        imageService.delete(owner, trip, post, image.getOriginalFileUrl());

        assertThat(imageRepository.findById(image.getId())).isEmpty();
    }

    @Test
    @DisplayName("소유자가 아니면 삭제할 수 없다")
    void deleteNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = createImage(owner, trip, post);

        assertThatThrownBy(() -> imageService.delete(other, trip, post, image.getId()))
            .isInstanceOf(ServiceException.class);
        assertThat(imageRepository.findById(image.getId())).isPresent();
    }

    @Test
    @DisplayName("이미지가 실제로 속한 post와 다른 post를 지정하면 삭제할 수 없다")
    void deleteWrongPost() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Post anotherPost = createPost(trip);
        Image image = createImage(owner, trip, post);

        assertThatThrownBy(() -> imageService.delete(owner, trip, anotherPost, image.getId()))
            .isInstanceOf(ServiceException.class);
        assertThat(imageRepository.findById(image.getId())).isPresent();
    }

    @Test
    @DisplayName("post 없이 삭제를 요청하면 post 일치 여부와 무관하게 삭제된다")
    void deleteWithoutPostScope() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = createImage(owner, trip, post);

        imageService.delete(owner, trip, null, image.getId());

        assertThat(imageRepository.findById(image.getId())).isEmpty();
    }

    @Test
    @DisplayName("post에 연결되지 않은 이미지를 특정 post로 삭제하려 하면 예외가 발생한다")
    void deleteUnassignedImageWithPostScope() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner);
        Post post = createPost(trip);
        Image image = createImage(owner, trip, null);

        assertThatThrownBy(() -> imageService.delete(owner, trip, post, image.getId()))
            .isInstanceOf(ServiceException.class);
        assertThat(imageRepository.findById(image.getId())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 id를 조회하면 예외가 발생한다")
    void getByIdNotFound() {
        assertThatThrownBy(() -> imageService.findById(-1L))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("존재하지 않는 url을 조회하면 예외가 발생한다")
    void getByUrlNotFound() {
        assertThatThrownBy(() -> imageService.findByUrl("https://example.com/none.jpg"))
            .isInstanceOf(ServiceException.class);
    }
}
