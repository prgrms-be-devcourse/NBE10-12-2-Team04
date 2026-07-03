package com.triptrace.domain.image.image.facade;

import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles({"test", "image-processor"})
public class ImageServiceFacadeTest {
    @Autowired
    ImageUploadFacade imageUploadFacade;
    @Autowired
    ImageDeleteFacade imageDeleteFacade;
    @Autowired
    ImageModifyFacade imageModifyFacade;
    @Autowired
    MemberRepository memberRepository;

    private Member owner;
    private Trip trip;
    private Post post;
    @Autowired
    TripService tripService;
    @Autowired
    TripRepository tripRepository;
    @Autowired
    PostRepository postRepository;

    @TempDir
    static Path tempDir;
    String imageFileName = "/test-a-mail.jpg";
    byte[] bytes;
    @Autowired
    private ImageService imageService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("custom.upload.path", () -> tempDir.toString());
        registry.add("custom.upload.serving", () -> "/serving");
        registry.add("custom.upload.thumbnail", () -> "/thumbnail");
        registry.add("custom.upload.profile", () -> "/profile");
    }

    @BeforeEach
    public void setUp() throws IOException {
        try (var is = getClass().getResourceAsStream(imageFileName)) {
            bytes = is.readAllBytes();
        }

        Member member = createMember("01username");
        this.owner = member;
        Trip trip = createTrip(member);
        this.trip = trip;
        Post post = postRepository.save(new Post(
            trip,
            LocalDate.of(2024, 4, 1),
            "첫날, 교토 도착",
            "교토에 도착했다."
        ));
        this.post = post;
    }

    private Member createMember(String username) {
        String hashedPassword = passwordEncoder.encode("password");
        return memberService.signup("test@email.com", username, hashedPassword, "url");
    }

    private Trip createTrip(Member member) {
        return tripRepository.save(new Trip(
            member,
            "교토 여행",
            "일본",
            "교토",
            LocalDateTime.of(2024, 4, 1, 0, 0),
            LocalDateTime.of(2024, 4, 5, 0, 0),
            true
        ));
    }

    private MultipartFile toMultipartFile() throws IOException {
        return new MockMultipartFile(
            "images",
            imageFileName,
            "image/jpeg",
            new ByteArrayInputStream(bytes)
        );
    }

    @Test
    @DisplayName("이미지를 업로드하면 저장 상태로 조회된다")
    void test01() throws IOException {
        MultipartFile[] files = new MultipartFile[]{toMultipartFile()};
        List<ImageUploadResponse> res = imageUploadFacade.uploadImages(owner.getId(), trip.getId(), files);

        assertThat(res.size()).isEqualTo(1);
        assertThat(res.getFirst().uploadStatus()).isEqualTo(UploadStatus.STORED);

        ImageServiceResponse imageServiceResponse = imageService.findById(res.getFirst().id());
        assertThat(imageServiceResponse).isNotNull();
        assertThat(imageServiceResponse.originalFileUrl()).startsWith("/serving/");
        assertThat(imageServiceResponse.thumbnailUrl()).startsWith("/thumbnail/");
    }

    @Test
    @DisplayName("이미지를 삭제하면 더 이상 조회되지 않는다")
    void test02() throws IOException {
        ImageUploadResponse uploaded = imageUploadFacade.uploadImage(
            owner.getId(), trip.getId(), post.getId(), toMultipartFile()
        );

        imageDeleteFacade.deleteById(owner.getId(), trip.getId(), post.getId(), uploaded.id());

        assertThatThrownBy(() -> imageService.findById(uploaded.id()))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("프로필 이미지를 업로드하면 회원의 프로필 URL이 갱신된다")
    void test03() throws IOException {
        String url = imageUploadFacade.uploadProfile(owner.getId(), toMultipartFile());

        assertThat(url).isNotBlank();
        assertThat(url).startsWith("/profile/");

        Member updated = memberRepository.findById(owner.getId()).orElseThrow();
        assertThat(updated.getProfileImageUrl()).isEqualTo(url);
    }
}
