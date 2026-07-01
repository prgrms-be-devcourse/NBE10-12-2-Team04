package com.triptrace.domain.trip.tripLike.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.entity.TripLike;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TripLikeServiceTest {
    @Autowired
    private TripLikeService tripLikeService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TripRepository tripRepository;

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

    @Test
    @DisplayName("중복 좋아요를 repository없이 service단에서만 테스트")
    public void duplicateLikeTest() throws Exception {
        Member member = createMember("member");
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "공개여행기");
        tripLikeService.createLike(member.getId(), trip.getId());
        assertThatThrownBy(() -> tripLikeService.createLike(member.getId(), trip.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("409-1 : 이미 좋아요한 여행기입니다.");
    }
}


