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

    @Test
    @WithMockUser
    @DisplayName("좋아요가 있는 공개,비공개 여행기를 세팅해놓고 좋아요 수 상위 10개 조회 테스트")
    public void getLikedTop10AllTrips() throws Exception {
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
        Trip trip10 = createTrip(owner10, "공개여행기10", true);

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

        mvc.perform(
                get("/api/v1/feed/trips/top-liked"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(10))
            .andExpect(jsonPath("$.data[0].title").value("공개여행기10"));
    }
}

