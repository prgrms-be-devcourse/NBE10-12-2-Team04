package com.triptrace.domain.trip.trip.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.dto.TripCreateRequest;
import com.triptrace.domain.trip.trip.dto.TripModifyRequest;
import com.triptrace.domain.trip.trip.dto.TripResponse;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
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
}
