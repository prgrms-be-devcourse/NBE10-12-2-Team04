package com.triptrace.domain.trip.tripLike.controller;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.repository.TripLikeRepository;
import com.triptrace.domain.trip.tripLike.service.TripLikeService;
import com.triptrace.global.exception.ServiceException;
import com.triptrace.global.globalExceptionHandler.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.RequestEntity.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RequestMapping("/api/v1/trips/{tripId}/likes")
public class TripLikeControllerTest {
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
    @Autowired
    private TripLikeRepository tripLikeRepository;

    @Test
    @WithMockUser(username = "test", roles = "USER")
    @DisplayName("좋아요 추가 및 좋아요 수 증가 테스트")
    public void t1() throws Exception {
        Member member = memberRepository.save(new Member(
            "test@test.com",
            "test",
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

        boolean exists = tripLikeRepository.existsByMemberIdAndTripId(member.getId(), trip.getId());
        assertThat(exists).isEqualTo(true);

        Trip found = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(found.getLikeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 취소 및 좋아요 수 감소 테스트")
    public void t2() throws Exception {
        Member member = memberRepository.save(new Member(
            "test@test.com",
            "test",
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

        tripLikeService.createLike(member.getId(), trip.getId());
        assertThat(trip.getLikeCount()).isEqualTo(1);

        tripLikeService.deleteLike(member.getId(), trip.getId());

        Boolean exists = tripLikeRepository.existsByMemberIdAndTripId(member.getId(), trip.getId());
        assertThat(exists).isFalse();
        assertThat(trip.getLikeCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("중복 좋아요 테스트")
    public void t3() throws Exception {
        Member member = memberRepository.save(new Member(
            "member@test.com",
            "member",
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

        tripLikeService.createLike(member.getId(), trip.getId());

        assertThatThrownBy(() -> tripLikeService.createLike(member.getId(), trip.getId()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("409-1 : 이미 좋아요한 여행기입니다.");
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    @DisplayName("좋아요 여부 조회 테스트")
    public void t4() throws Exception {
        Member member = memberRepository.save(new Member(
            "member@test.com",
            "member",
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

        tripLikeService.createLike(member.getId(), trip.getId());

        mvc.perform(MockMvcRequestBuilders
                .get("/api/v1/trips/" + trip.getId() + "/likes/me")
                .param("memberId", String.valueOf(member.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("좋아요 여부 조회 성공했습니다."))
            .andExpect(jsonPath("$.data.liked").value(true))
            .andDo(print());
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    @DisplayName("좋아요 하지 않는 여행기를 취소하려는 경우 예외처리 테스트")
    public void t5() throws Exception {
        Member member = memberRepository.save(new Member(
            "member@test.com",
            "member",
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

        mvc.perform(
            delete("/api/v1/trips/" + trip.getId() + "/likes")
                .param("memberId", String.valueOf(member.getId()))
                .with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.resultCode").value("404-1"))
            .andExpect(jsonPath("$.msg").value("좋아요한 적이 없는 여행기입니다."))
            .andDo(print());
    }
}
