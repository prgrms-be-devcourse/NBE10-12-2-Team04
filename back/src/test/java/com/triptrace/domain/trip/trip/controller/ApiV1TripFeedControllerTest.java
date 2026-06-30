package com.triptrace.domain.trip.trip.controller;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.domain.trip.tripLike.entity.TripLike;
import com.triptrace.domain.trip.tripLike.service.TripLikeService;
import lombok.AccessLevel;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ApiV1TripFeedControllerTest {
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private TripService tripService;
    @Autowired
    private TripLikeService tripLikeService;

    @BeforeEach
    public void setup() {
        Member member = memberRepository.save(new Member(
            "test@test.com",
            "member1",
            "password1234",
            UUID.randomUUID().toString(),
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Trip trip = tripRepository.save(new Trip(
            member,
            "title1",
            "country1",
            "city1",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        when(member.getId()).thenReturn(1L);
        when(trip.getId()).thenReturn(1L);
        tripLikeService.createLike(member.getId(), trip.getId());
        when(member.getId()).thenReturn(2L);
        tripLikeService.createLike(member.getId(), trip.getId());

        when(member.getId()).thenReturn(2L);
        when(trip.getId()).thenReturn(2L);
        tripLikeService.createLike(member.getId(), trip.getId());
    }

    @Test
    @DisplayName("좋아요 Top10 조회 테스트")
    public void getLikedTop10() throws Exception {

    }
}

