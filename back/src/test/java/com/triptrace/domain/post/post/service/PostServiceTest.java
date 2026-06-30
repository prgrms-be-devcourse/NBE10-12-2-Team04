package com.triptrace.domain.post.post.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.dto.PostCreateRequest;
import com.triptrace.domain.post.post.dto.PostModifyRequest;
import com.triptrace.domain.post.post.dto.PostResponse;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PostServiceTest {
    @Autowired
    private PostService postService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("소유자는 여행기에 게시물을 생성할 수 있다.")
    void create() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");

        PostResponse response = postService.create(trip.getId(), owner.getId(), new PostCreateRequest(
            LocalDate.of(2026, 1, 2),
            "둘째 날",
            "아라시야마에 갔다."
        ));

        Post found = postRepository.findById(response.id()).orElseThrow();
        assertThat(found.getTrip().getId()).isEqualTo(trip.getId());
        assertThat(found.getDate()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(found.getTitle()).isEqualTo("둘째 날");
    }

    @Test
    @DisplayName("소유자가 아니면 게시물을 생성할 수 없다.")
    void createByNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner, "교토 여행");

        assertThatThrownBy(() -> postService.create(trip.getId(), other.getId(), new PostCreateRequest(
            LocalDate.of(2026, 1, 2),
            "둘째 날",
            "아라시야마에 갔다."
        )))
            .isInstanceOf(ServiceException.class)
            .hasMessage("403-1 : 여행기에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("여행기 게시물 목록을 날짜 오름차순으로 조회한다.")
    void findPostsByTripId() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");
        createPost(trip, LocalDate.of(2026, 1, 3), "셋째 날");
        createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");
        createPost(trip, LocalDate.of(2026, 1, 2), "둘째 날");

        List<PostResponse> responses = postService.findPostsByTripId(trip.getId(), owner.getId());

        assertThat(responses)
            .extracting(PostResponse::title)
            .containsExactly("첫째 날", "둘째 날", "셋째 날");
    }

    @Test
    @DisplayName("공개 여행기의 게시물은 ownerId 없이 상세 조회할 수 있다.")
    void findPublicPost() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "공개 여행기");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");

        PostResponse response = postService.findAccessiblePost(post.getId(), null);

        assertThat(response.title()).isEqualTo("첫째 날");
    }

    @Test
    @DisplayName("비공개 여행기의 게시물은 소유자만 상세 조회할 수 있다.")
    void findPrivatePostByOwner() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "비공개 여행기", false);
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");

        PostResponse response = postService.findAccessiblePost(post.getId(), owner.getId());

        assertThat(response.title()).isEqualTo("첫째 날");
    }

    @Test
    @DisplayName("소유자가 아니면 비공개 여행기의 게시물을 상세 조회할 수 없다.")
    void findPrivatePostByNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner, "비공개 여행기", false);
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");

        assertThatThrownBy(() -> postService.findAccessiblePost(post.getId(), other.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("403-1 : 여행기에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("소유자는 게시물을 수정할 수 있다.")
    void modifyByOwner() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "수정 전");

        PostResponse response = postService.modifyPost(post.getId(), owner.getId(), new PostModifyRequest(
            LocalDate.of(2026, 1, 2),
            "수정 후",
            "수정된 메모"
        ));

        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(response.title()).isEqualTo("수정 후");
        assertThat(response.memo()).isEqualTo("수정된 메모");
    }

    @Test
    @DisplayName("소유자는 게시물을 삭제할 수 있다.")
    void deleteByOwner() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "삭제할 게시물");

        postService.deletePost(post.getId(), owner.getId());

        assertThat(postRepository.existsById(post.getId())).isFalse();
    }

    private Member createMember(String username) {
        return memberRepository.save(new Member(
            "%s@test.com".formatted(username),
            username,
            "password1234",
            UUID.randomUUID().toString(),
            "imageUrl",
            MemberStatus.ACTIVE
        ));
    }

    private Trip createTrip(Member owner, String title) {
        return createTrip(owner, title, true);
    }

    private Trip createTrip(Member owner, String title, boolean visibility) {
        return tripRepository.save(new Trip(
            owner,
            title,
            "일본",
            "교토",
            LocalDateTime.of(2026, 1, 1, 0, 0),
            LocalDateTime.of(2026, 1, 5, 0, 0),
            visibility
        ));
    }

    private Post createPost(Trip trip, LocalDate date, String title) {
        return postRepository.save(new Post(
            trip,
            date,
            title,
            "교토 여행 메모"
        ));
    }
}
