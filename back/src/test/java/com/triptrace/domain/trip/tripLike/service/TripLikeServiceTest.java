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
    private Member member = Mockito.mock(Member.class);
    private Trip trip = Mockito.mock(Trip.class);
    private TripLikeService tripLikeService = Mockito.mock(TripLikeService.class);
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TripRepository tripRepository;

//    @BeforeEach
//    public void setup() {
//        Member member = memberRepository.save(new Member(
//            "test@test.com",
//            "member",
//            "password1234",
//            UUID.randomUUID().toString(),
//            "imageUrl",
//            MemberStatus.ACTIVE
//        ));
//
//        Trip trip = tripRepository.save(new Trip(
//            member,
//            "test title",
//            "test country",
//            "test city",
//            LocalDateTime.now().minusMonths(12),
//            LocalDateTime.now().minusMonths(6),
//            true
//        ));
//    }

    @Test
    @DisplayName("중복 좋아요를 repository없이 service단에서만 테스트")
    public void duplicateLikeTest() throws Exception {
        tripLikeService.createLike(member.getId(), trip.getId());
        assertThatThrownBy(() -> tripLikeService.createLike(member.getId(), trip.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("409-1", "이미 좋아요한 여행기입니다.");
    }

    @Test
    @DisplayName("중복 좋아요 테스트중 Mokito를 사용해서 가짜객체 생성이 가능하고 ")
    public void duplicateLikeTest2() throws Exception {
        tripLikeService.createLike(member.getId(), trip.getId());
        tripLikeService.createLike(member.getId(), trip.getId());
        tripLikeService.createLike(member.getId(), trip.getId());
    }
}


