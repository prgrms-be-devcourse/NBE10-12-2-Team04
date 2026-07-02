package com.triptrace.domain.post.post.controller;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1PostControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    @WithMockUser
    @DisplayName("게시물 생성 API")
    void create() throws Exception {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");

        mvc.perform(post("/api/v1/trips/{tripId}/posts", trip.getId())
                .with(csrf())
                .with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "2026-01-02",
                      "title": "둘째 날",
                      "memo": "아라시야마에 갔다."
                    }
                    """))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.data.tripId").value(trip.getId()))
            .andExpect(jsonPath("$.data.title").value("둘째 날"));
    }

    @Test
    @WithMockUser
    @DisplayName("여행기 게시물 목록 조회 API")
    void getPosts() throws Exception {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");
        createPost(trip, LocalDate.of(2026, 1, 2), "둘째 날");
        createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");

        mvc.perform(get("/api/v1/trips/{tripId}/posts", trip.getId())
                .with(authentication(auth(owner))))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].title").value("첫째 날"))
            .andExpect(jsonPath("$.data[1].title").value("둘째 날"));
    }

    @Test
    @WithMockUser
    @DisplayName("공개 여행기 게시물 상세 조회 API")
    void getPost() throws Exception {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "공개 여행기");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");

        mvc.perform(get("/api/v1/posts/{postId}", post.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(post.getId()))
            .andExpect(jsonPath("$.data.title").value("첫째 날"));
    }

    @Test
    @WithMockUser
    @DisplayName("소유자가 아닌 사용자는 비공개 여행기 게시물을 조회할 수 없다.")
    void getPrivatePostForbidden() throws Exception {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner, "비공개 여행기", false);
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "첫째 날");

        mvc.perform(get("/api/v1/posts/{postId}", post.getId())
                .with(authentication(auth(other))))
            .andDo(print())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @WithMockUser
    @DisplayName("게시물 수정 API")
    void modifyPost() throws Exception {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "교토 여행");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "수정 전");

        mvc.perform(patch("/api/v1/posts/{postId}", post.getId())
                .with(csrf())
                .with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "2026-01-02",
                      "title": "수정 후",
                      "memo": "수정된 메모"
                    }
                    """))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.date").value("2026-01-02"))
            .andExpect(jsonPath("$.data.title").value("수정 후"));
    }

    @Test
    @WithMockUser
    @DisplayName("소유자가 아닌 사용자는 게시물을 삭제할 수 없다.")
    void deleteForbidden() throws Exception {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner, "교토 여행");
        Post post = createPost(trip, LocalDate.of(2026, 1, 1), "삭제 방어");

        mvc.perform(delete("/api/v1/posts/{postId}", post.getId())
                .with(csrf())
                .with(authentication(auth(other))))
            .andDo(print())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.resultCode").value("403-1"));

        assertThat(postRepository.existsById(post.getId())).isTrue();
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

    private Post createPost(Trip trip, LocalDate date, String title) {
        return postRepository.save(new Post(
            trip,
            date,
            title,
            "교토 여행 메모"
        ));
    }

    private UsernamePasswordAuthenticationToken auth(Member member) {
        return new UsernamePasswordAuthenticationToken(member.getId(), null, List.of());
    }
}
