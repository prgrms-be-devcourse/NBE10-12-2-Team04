package com.triptrace.domain.trip.tripLike.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RequestMapping("/api/v1/trips/{tripId}/likes")
public class ApiV1TripLikeControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private TripLikeService tripLikeService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TripRepository tripRepository;

    @Test
    @WithMockUser(username = "test1", roles = "USER")
    @DisplayName("좋아요 추가 기능에 성공하면 좋아요 수가 1 증가한다.")
    public void t1() throws Exception {
        Member member = memberRepository.save(new Member(
            "test@test.com",
            "test1",
            "password1234",
            UUID.randomUUID().toString(),
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Trip trip = tripRepository.save(new Trip(
            member,
            "test title",
            "test country",
            "test city",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        mockMvc.perform(post("/api/v1/trips/{tripId}/likes", trip.getId())
                .param("memberId", String.valueOf(member.getId()))
                .with(csrf()))
            .andDo(print())
            .andExpect(status().isCreated());

        Trip found = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(found.getLikeCount()).isEqualTo(1);
    }
}



