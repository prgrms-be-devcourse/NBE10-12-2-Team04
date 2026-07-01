package com.triptrace.domain.trip.trip.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.dto.TripCreateRequest;
import com.triptrace.domain.trip.trip.dto.TripModifyRequest;
import com.triptrace.domain.trip.trip.dto.TripResponse;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.service.TripLikeService;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class TripServiceTest {
    @Autowired
    private TripService tripService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripLikeService tripLikeService;

    @Test
    @DisplayName("ownerId를 받아 여행기를 생성한다.")
    void create() {
        Member member = createMember("user1");

        TripResponse response = tripService.create(member.getId(), new TripCreateRequest(
            "교토 여행",
            "일본",
            "교토",
            LocalDateTime.of(2026, 1, 1, 0, 0),
            LocalDateTime.of(2026, 1, 5, 0, 0),
            true
        ));

        Trip found = tripRepository.findById(response.id()).orElseThrow();
        assertThat(found.getOwner().getId()).isEqualTo(member.getId());
        assertThat(found.getTitle()).isEqualTo("교토 여행");
        assertThat(found.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("ownerId 기준으로 내 여행기 목록을 조회한다.")
    void findTripsByOwnerId() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        createTrip(owner, "내 여행기 1");
        createTrip(owner, "내 여행기 2");
        createTrip(other, "다른 사람 여행기");

        List<TripResponse> responses = tripService.findTripsByOwnerId(owner.getId());

        assertThat(responses).hasSize(2);
        assertThat(responses)
            .extracting(TripResponse::title)
            .containsExactlyInAnyOrder("내 여행기 1", "내 여행기 2");
    }

    @Test
    @DisplayName("공개 여행기 목록만 조회한다.")
    void findPublicTrips() {
        Member owner = createMember("owner");
        createTrip(owner, "공개 여행기");
        createTrip(owner, "비공개 여행기", false);

        List<TripResponse> responses = tripService.findPublicTrips();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("공개 여행기");
    }

    @Test
    @DisplayName("공개 여행기는 ownerId 없이 상세 조회할 수 있다.")
    void findPublicTrip() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "공개 여행기");

        TripResponse response = tripService.findAccessibleTrip(trip.getId(), null);

        assertThat(response.title()).isEqualTo("공개 여행기");
    }

    @Test
    @DisplayName("비공개 여행기는 소유자만 상세 조회할 수 있다.")
    void findPrivateTripByOwner() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "비공개 여행기", false);

        TripResponse response = tripService.findAccessibleTrip(trip.getId(), owner.getId());

        assertThat(response.title()).isEqualTo("비공개 여행기");
    }

    @Test
    @DisplayName("소유자가 아니면 비공개 여행기를 상세 조회할 수 없다.")
    void findPrivateTripByNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner, "비공개 여행기", false);

        assertThatThrownBy(() -> tripService.findAccessibleTrip(trip.getId(), other.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("403-1 : 여행기에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("소유자는 공개 여부와 무관하게 Trip 엔티티를 조회할 수 있다.")
    void findOwnedTripByOwner() {
        Member owner = createMember("owner");
        Trip privateTrip = createTrip(owner, "이미지 업로드 대상 여행기", false);

        Trip found = tripService.findOwnedTrip(privateTrip.getId(), owner.getId());

        assertThat(found.getId()).isEqualTo(privateTrip.getId());
        assertThat(found.getTitle()).isEqualTo("이미지 업로드 대상 여행기");
    }

    @Test
    @DisplayName("소유자가 아니면 공개 여행기도 내부 처리용 Trip 엔티티로 조회할 수 없다.")
    void findOwnedTripByNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip publicTrip = createTrip(owner, "공개 여행기");

        assertThatThrownBy(() -> tripService.findOwnedTrip(publicTrip.getId(), other.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("403-1 : 여행기에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("소유자는 여행기를 수정할 수 있다.")
    void modifyByOwner() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "수정 전");

        TripResponse response = tripService.modifyTrip(trip.getId(), owner.getId(), new TripModifyRequest(
            "수정 후",
            "한국",
            "서울",
            LocalDateTime.of(2026, 2, 1, 0, 0),
            LocalDateTime.of(2026, 2, 3, 0, 0),
            false
        ));

        assertThat(response.title()).isEqualTo("수정 후");
        assertThat(response.country()).isEqualTo("한국");
        assertThat(response.city()).isEqualTo("서울");
        assertThat(response.visibility()).isFalse();
    }

    @Test
    @DisplayName("소유자가 아니면 여행기를 수정할 수 없다.")
    void modifyByNotOwner() {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner, "수정 전");

        assertThatThrownBy(() -> tripService.modifyTrip(trip.getId(), other.getId(), new TripModifyRequest(
            "수정 시도",
            "한국",
            "서울",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            true
        )))
            .isInstanceOf(ServiceException.class)
            .hasMessage("403-1 : 여행기에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("소유자는 여행기를 삭제할 수 있다.")
    void deleteByOwner() {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "삭제할 여행기");

        tripService.deleteTrip(trip.getId(), owner.getId());

        assertThat(tripRepository.existsById(trip.getId())).isFalse();
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


    // 공개여행기 좋아요 수 상위 10개 조회 테스트
    @Test
    @DisplayName("좋아요가 있는 공개여행기 중 좋아요수 기준 상위 10개 게시물 조회")
    public void findTop10PublicTripsByLikeCount() {
        Member member1 = createMember("member1");
        Member member2 = createMember("member2");
        Member member3 = createMember("member3");
        Member member4 = createMember("member4");
        Member member5 = createMember("member5");
        Member member6 = createMember("member6");
        Member member7 = createMember("member7");
        Member member8 = createMember("member8");
        Member member9 = createMember("member9");
        Member member10 = createMember("member10");

        Member owner1 = createMember("owner1");
        Member owner2 = createMember("owner2");
        Member owner3 = createMember("owner3");
        Member owner4 = createMember("owner4");
        Member owner5 = createMember("owner5");
        Member owner6 = createMember("owner6");
        Member owner7 = createMember("owner7");
        Member owner8 = createMember("owner8");
        Member owner9 = createMember("owner9");
        Member owner10 = createMember("owner10");

        Trip trip1 = createTrip(owner1, "공개여행기1", true);
        Trip trip2 = createTrip(owner2, "공개여행기2", true);
        Trip trip3 = createTrip(owner3, "공개여행기3", true);
        Trip trip4 = createTrip(owner4, "공개여행기4", true);
        Trip trip5 = createTrip(owner5, "공개여행기5", true);
        Trip trip6 = createTrip(owner6, "공개여행기6", true);
        Trip trip7 = createTrip(owner7, "공개여행기7", true);
        Trip trip8 = createTrip(owner8, "공개여행기8", true);
        Trip trip9 = createTrip(owner9, "공개여행기9", true);
        Trip trip10 = createTrip(owner10, "비공개여행기", false);

        tripLikeService.createLike(member1.getId(), trip10.getId());
        tripLikeService.createLike(member2.getId(), trip10.getId());
        tripLikeService.createLike(member3.getId(), trip10.getId());
        tripLikeService.createLike(member4.getId(), trip10.getId());
        tripLikeService.createLike(member5.getId(), trip10.getId());
        tripLikeService.createLike(member6.getId(), trip10.getId());
        tripLikeService.createLike(member7.getId(), trip10.getId());
        tripLikeService.createLike(member8.getId(), trip10.getId());
        tripLikeService.createLike(member9.getId(), trip10.getId());
        tripLikeService.createLike(member10.getId(), trip10.getId());
        tripLikeService.createLike(member1.getId(), trip9.getId());
        tripLikeService.createLike(member2.getId(), trip9.getId());
        tripLikeService.createLike(member3.getId(), trip9.getId());
        tripLikeService.createLike(member4.getId(), trip9.getId());
        tripLikeService.createLike(member5.getId(), trip9.getId());
        tripLikeService.createLike(member6.getId(), trip9.getId());
        tripLikeService.createLike(member7.getId(), trip9.getId());
        tripLikeService.createLike(member8.getId(), trip9.getId());
        tripLikeService.createLike(member9.getId(), trip9.getId());
        tripLikeService.createLike(member1.getId(), trip8.getId());
        tripLikeService.createLike(member2.getId(), trip8.getId());
        tripLikeService.createLike(member3.getId(), trip8.getId());
        tripLikeService.createLike(member4.getId(), trip8.getId());
        tripLikeService.createLike(member5.getId(), trip8.getId());
        tripLikeService.createLike(member6.getId(), trip8.getId());
        tripLikeService.createLike(member7.getId(), trip8.getId());
        tripLikeService.createLike(member8.getId(), trip8.getId());
        tripLikeService.createLike(member1.getId(), trip7.getId());
        tripLikeService.createLike(member2.getId(), trip7.getId());
        tripLikeService.createLike(member3.getId(), trip7.getId());
        tripLikeService.createLike(member4.getId(), trip7.getId());
        tripLikeService.createLike(member5.getId(), trip7.getId());
        tripLikeService.createLike(member6.getId(), trip7.getId());
        tripLikeService.createLike(member7.getId(), trip7.getId());
        tripLikeService.createLike(member1.getId(), trip6.getId());
        tripLikeService.createLike(member2.getId(), trip6.getId());
        tripLikeService.createLike(member3.getId(), trip6.getId());
        tripLikeService.createLike(member4.getId(), trip6.getId());
        tripLikeService.createLike(member5.getId(), trip6.getId());
        tripLikeService.createLike(member6.getId(), trip6.getId());
        tripLikeService.createLike(member1.getId(), trip5.getId());
        tripLikeService.createLike(member2.getId(), trip5.getId());
        tripLikeService.createLike(member3.getId(), trip5.getId());
        tripLikeService.createLike(member4.getId(), trip5.getId());
        tripLikeService.createLike(member5.getId(), trip5.getId());
        tripLikeService.createLike(member1.getId(), trip4.getId());
        tripLikeService.createLike(member2.getId(), trip4.getId());
        tripLikeService.createLike(member3.getId(), trip4.getId());
        tripLikeService.createLike(member4.getId(), trip4.getId());
        tripLikeService.createLike(member1.getId(), trip3.getId());
        tripLikeService.createLike(member2.getId(), trip3.getId());
        tripLikeService.createLike(member3.getId(), trip3.getId());
        tripLikeService.createLike(member1.getId(), trip2.getId());
        tripLikeService.createLike(member2.getId(), trip2.getId());
        tripLikeService.createLike(member1.getId(), trip1.getId());

        List<TripResponse> tripList = tripService.findTop10PublicTripsByLikeCount();

        assertThat(tripList.getFirst().likeCount()).isEqualTo(9);
    }
}
