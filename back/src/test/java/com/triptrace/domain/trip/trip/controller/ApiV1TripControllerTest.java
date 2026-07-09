package com.triptrace.domain.trip.trip.controller;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
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

import java.time.LocalDateTime;
import java.util.List;

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
class ApiV1TripControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Test
    @WithMockUser
    @DisplayName("여행기 생성 API")
    void create() throws Exception {
        Member member = createMember("user1");

        mvc.perform(post("/api/v1/trips")
                .with(csrf())
                .with(authentication(auth(member)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "교토 여행",
                      "country": "일본",
                      "city": "교토",
                      "startDate": "2026-01-01T00:00:00",
                      "endDate": "2026-01-05T00:00:00",
                      "visibility": true
                    }
                    """))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.data.ownerId").value(member.getId()))
            .andExpect(jsonPath("$.data.title").value("교토 여행"));
    }

    @Test
    @WithMockUser
    @DisplayName("내 여행기 목록 조회 API")
    void getMyTrips() throws Exception {
        Member owner = createMember("owner");
        Member other = createMember("other");
        createTrip(owner, "내 여행기");
        createTrip(other, "다른 사람 여행기");

        mvc.perform(get("/api/v1/users/me/trips")
                .with(authentication(auth(owner))))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].title").value("내 여행기"));
    }

    @Test
    @WithMockUser
    @DisplayName("전체 여행기 목록 조회 API")
    void getTrips() throws Exception {
        Member owner = createMember("owner");
        Member other = createMember("other");
        createTrip(owner, "공개 여행기");
        createTrip(other, "비공개 여행기", false);

        mvc.perform(get("/api/v1/trips"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].title").value("공개 여행기"));
    }

    @Test
    @WithMockUser
    @DisplayName("공개 여행기는 ownerId 없이 상세 조회할 수 있다.")
    void getTrip() throws Exception {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "상세 여행기");

        mvc.perform(get("/api/v1/trips/{tripId}", trip.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(trip.getId()))
            .andExpect(jsonPath("$.data.title").value("상세 여행기"));
    }

    @Test
    @WithMockUser
    @DisplayName("비공개 여행기는 소유자만 상세 조회할 수 있다.")
    void getPrivateTripByOwner() throws Exception {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "비공개 여행기", false);

        mvc.perform(get("/api/v1/trips/{tripId}", trip.getId())
                .with(authentication(auth(owner))))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(trip.getId()))
            .andExpect(jsonPath("$.data.title").value("비공개 여행기"));
    }

    @Test
    @WithMockUser
    @DisplayName("소유자가 아닌 사용자는 비공개 여행기를 상세 조회할 수 없다.")
    void getPrivateTripForbidden() throws Exception {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner, "비공개 여행기", false);

        mvc.perform(get("/api/v1/trips/{tripId}", trip.getId())
                .with(authentication(auth(other))))
            .andDo(print())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @WithMockUser
    @DisplayName("여행기 수정 API")
    void modifyTrip() throws Exception {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "수정 전");

        mvc.perform(patch("/api/v1/trips/{tripId}", trip.getId())
                .with(csrf())
                .with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "수정 후",
                      "country": "한국",
                      "city": "서울",
                      "startDate": "2026-02-01T00:00:00",
                      "endDate": "2026-02-03T00:00:00",
                      "visibility": false
                    }
                    """))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("수정 후"))
            .andExpect(jsonPath("$.data.visibility").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("소유자가 아닌 사용자는 여행기를 삭제할 수 없다.")
    void deleteForbidden() throws Exception {
        Member owner = createMember("owner");
        Member other = createMember("other");
        Trip trip = createTrip(owner, "삭제 방어");

        mvc.perform(delete("/api/v1/trips/{tripId}", trip.getId())
                .with(csrf())
                .with(authentication(auth(other))))
            .andDo(print())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.resultCode").value("403-1"));

        assertThat(tripRepository.existsById(trip.getId())).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("여행기 대표이미지 변경 API")
    void changeRepresentativeImage() throws Exception {
        Member owner = createMember("owner");
        Trip trip = createTrip(owner, "대표이미지 변경 여행기");
        Image image = toEntity(owner, trip, "representative.jpg");

        mvc.perform(patch("/api/v1/trips/{tripId}/representative-image", trip.getId())
                .with(csrf())
                .with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "imageId": %d
                    }
                    """.formatted(image.getId())))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.thumbnailUrl").value("/images/thumbnail/representative.jpg"));
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

    private Image toEntity(Member owner, Trip trip, String fileName) {
        return imageRepository.save(new Image(
            owner,
            trip,
            null,
            "/images/serving/%s".formatted(fileName),
            "/images/thumbnail/%s".formatted(fileName),
            1024L,
            "image/jpeg",
            UploadStatus.STORED
        ));
    }

    private UsernamePasswordAuthenticationToken auth(Member member) {
        return new UsernamePasswordAuthenticationToken(member.getId(), null, List.of());
    }
}
