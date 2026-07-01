package com.triptrace.domain.trip.trip.controller;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.service.TripLikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public class ApiV1TripFeedControllerTest {
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private TripLikeService tripLikeService;
    @Autowired
    private MockMvc mvc;

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

    public List<Member> creatMemberList(int num) {
        List<Member> memberList = new ArrayList();
        for (int i = 1; i <= num; i++) {
            memberList.add(createMember("member%d".formatted(i)));
        }
        return memberList;
    }

    public List<Trip> creatTripList(int num) {
        List<Member> ownerList = new ArrayList();
        List<Trip> tripList = new ArrayList();
        for (int i = 1; i <= num; i++) {
            ownerList.add(createMember("owner%d".formatted(i)));
            tripList.add(createTrip(ownerList.get(i - 1), "공개여행기%d".formatted(i)));
        }
        return tripList;
    }



    @Test
    @WithMockUser
    @DisplayName("좋아요가 있는 공개,비공개 여행기를 세팅해놓고 좋아요 수 상위 10개 조회 테스트")
    public void getLikedTop10AllTrips() throws Exception {
        List<Member> memberList = creatMemberList(10);
        List<Trip> tripList = creatTripList(9);
        Member owner10 = createMember("owner10");
        Trip trip10 = createTrip(owner10, "비공개여행기", false);

        tripLikeService.createLike(memberList.get(0).getId(), trip10.getId());
        tripLikeService.createLike(memberList.get(1).getId(), trip10.getId());
        tripLikeService.createLike(memberList.get(2).getId(), trip10.getId());
        tripLikeService.createLike(memberList.get(3).getId(), trip10.getId());
        tripLikeService.createLike(memberList.get(4).getId(), trip10.getId());
        tripLikeService.createLike(memberList.get(5).getId(), trip10.getId());
        tripLikeService.createLike(memberList.get(6).getId(), trip10.getId());
        tripLikeService.createLike(memberList.get(7).getId(), trip10.getId());
        tripLikeService.createLike(memberList.get(8).getId(), trip10.getId());
        tripLikeService.createLike(memberList.get(9).getId(), trip10.getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(5).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(6).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(7).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(8).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(5).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(6).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(7).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(5).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(6).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(5).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(4).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(4).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(4).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(4).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(4).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(3).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(3).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(3).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(3).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(2).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(2).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(2).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(1).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(1).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(0).getId());

        mvc.perform(
            get("/api/v1/feed/trips/top-liked"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(9))
            .andExpect(jsonPath("$.data[0].title").value("공개여행기9"));
    }

    @Test
    @WithMockUser
    @DisplayName("좋아요 Top10 조회 테스트")
    public void getLikedTop10VisibilityTrue() throws Exception {
        List<Member> memberList = creatMemberList(10);
        List<Trip> tripList = creatTripList(10);

        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(9).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(9).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(9).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(9).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(9).getId());
        tripLikeService.createLike(memberList.get(5).getId(), tripList.get(9).getId());
        tripLikeService.createLike(memberList.get(6).getId(), tripList.get(9).getId());
        tripLikeService.createLike(memberList.get(7).getId(), tripList.get(9).getId());
        tripLikeService.createLike(memberList.get(8).getId(), tripList.get(9).getId());
        tripLikeService.createLike(memberList.get(9).getId(), tripList.get(9).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(5).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(6).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(7).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(8).getId(), tripList.get(8).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(5).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(6).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(7).getId(), tripList.get(7).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(5).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(6).getId(), tripList.get(6).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(5).getId(), tripList.get(5).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(4).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(4).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(4).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(4).getId());
        tripLikeService.createLike(memberList.get(4).getId(), tripList.get(4).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(3).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(3).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(3).getId());
        tripLikeService.createLike(memberList.get(3).getId(), tripList.get(3).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(2).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(2).getId());
        tripLikeService.createLike(memberList.get(2).getId(), tripList.get(2).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(1).getId());
        tripLikeService.createLike(memberList.get(1).getId(), tripList.get(1).getId());
        tripLikeService.createLike(memberList.get(0).getId(), tripList.get(0).getId());

        mvc.perform(
                get("/api/v1/feed/trips/top-liked"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(10))
            .andExpect(jsonPath("$.data[0].title").value("공개여행기10"));
    }
}

