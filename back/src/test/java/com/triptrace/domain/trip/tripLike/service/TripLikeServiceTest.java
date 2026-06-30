package com.triptrace.domain.trip.tripLike.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TripLikeServiceTest {
    private Member member = Mockito.mock(Member.class);
    private Trip trip = Mockito.mock(Trip.class);
    private TripLikeService tripLikeService = Mockito.mock(TripLikeService.class);

    @Test
    @DisplayName("중복 좋아요를 repository없이 service단에서만 테스트")
    public void duplicateLikeTest() throws Exception {
        tripLikeService.createLike(member.getId(), trip.getId());
        assertThatThrownBy(() -> tripLikeService.createLike(member.getId(), trip.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("409-1", "이미 좋아요한 여행기입니다.");
    }

    @Test
    @DisplayName("중복 좋아요 테스트중")
    public void duplicateLikeTest2() throws Exception {
        tripLikeService.createLike(member.getId(), trip.getId());
        tripLikeService.createLike(member.getId(), trip.getId());
    }
}
