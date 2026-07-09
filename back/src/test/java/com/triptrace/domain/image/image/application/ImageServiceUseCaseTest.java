package com.triptrace.domain.image.image.application;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.response.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.exception.ServiceException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ImageServiceUseCaseTest {
    @Autowired
    ImageUploadUseCase imageUploadUseCase;
    @Autowired
    ImageDeleteUseCase imageDeleteUseCase;
    @Autowired
    ImageModifyUseCase imageModifyUseCase;

    private Member owner;
    private Trip trip;
    private Post post;
    private Post otherPost;
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
        registry.add("custom.upload.serving-path", () -> "/images/serving");
        registry.add("custom.upload.thumbnail-path", () -> "/images/thumbnail");
        registry.add("custom.upload.profile-path", () -> "/images/profile");
        registry.add("custom.upload.public-prefix", () -> "/");
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
        Post post = postRepository.save(
            new Post(
                trip,
                LocalDate.of(
                    2024,
                    4,
                    1),
                "첫날, 교토 도착",
                "교토에 도착했다."));
        Post post2 = postRepository.save(
            new Post(
                trip,
                LocalDate.of(
                    2024,
                    4,
                    1),
                "첫날, 교토 도착",
                "교토에 도착했다."));
        this.post = post;
        this.otherPost = post2;
    }

    private Member createMember(String username) {
        String hashedPassword = passwordEncoder.encode("password");
        return memberService.signup("test@email.com", username, hashedPassword, "url");
    }

    private Trip createTrip(Member member) {
        return tripRepository.save(
            new Trip(
                member,
                "교토 여행",
                "일본",
                "교토",
                LocalDateTime.of(
                    2024,
                    4,
                    1,
                    0,
                    0),
                LocalDateTime.of(
                    2024,
                    4,
                    5,
                    0,
                    0),
                true));
    }

    private MultipartFile toMultipartFile() throws IOException {
        return new MockMultipartFile(
            "images",
            imageFileName,
            "image/jpeg",
            new ByteArrayInputStream(bytes));
    }

    @Test
    @DisplayName("이미지를 업로드하면 저장 상태로 조회된다")
    void test01() throws IOException {
        MultipartFile[] files = new MultipartFile[] {toMultipartFile()};
        List<ImageUploadResponse> res = imageUploadUseCase.uploadImages(owner.getId(), trip.getId(), files);

        assertThat(res.size()).isEqualTo(1);
        assertThat(res.getFirst().uploadStatus()).isEqualTo(UploadStatus.STORED);

        ImageServiceResponse imageServiceResponse = imageService.findById(res.getFirst().id());
        assertThat(imageServiceResponse).isNotNull();
        assertThat(imageServiceResponse.originalFileUrl()).startsWith("/images/serving/");
        assertThat(imageServiceResponse.thumbnailUrl()).startsWith("/images/thumbnail/");
    }

    @Test
    @DisplayName("이미지를 삭제하면 더 이상 조회되지 않는다")
    void test02() throws IOException {
        MultipartFile[] files = new MultipartFile[] {toMultipartFile()};
        List<ImageUploadResponse> uploadImages = imageUploadUseCase.uploadImages(owner.getId(), trip.getId(),
            post.getId(), files);
        imageDeleteUseCase.deleteById(owner.getId(), trip.getId(),
            post.getId(), uploadImages.getFirst().id());
        ImageUploadResponse uploaded = uploadImages.stream().findFirst().get();

        assertThatThrownBy(() -> imageService.findById(uploaded.id())).isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("PostId와 함께 이미지를 업로드하면 PostId와 함께 저장 상태로 조회된다")
    void test03() throws IOException {
        MultipartFile[] files = new MultipartFile[] {toMultipartFile()};
        List<ImageUploadResponse> res = imageUploadUseCase.uploadImages(owner.getId(), trip.getId(), post.getId(),
            files);

        assertThat(res.size()).isEqualTo(1);
        assertThat(res.getFirst().uploadStatus()).isEqualTo(UploadStatus.STORED);

        ImageServiceResponse imageServiceResponse = imageService.findById(res.getFirst().id());
        assertThat(imageServiceResponse).isNotNull();
        assertThat(imageServiceResponse.originalFileUrl()).startsWith("/images/serving/");
        assertThat(imageServiceResponse.thumbnailUrl()).startsWith("/images/thumbnail/");

        Image image = imageService.getById(res.getFirst().id());
        assertThat(image).isNotNull();
        assertThat(image.getPost().getId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("Post 없이 이미지를 삭제하면 더 이상 조회되지 않는다.")
    void test04() throws IOException {
        MultipartFile[] files = new MultipartFile[] {toMultipartFile()};
        List<ImageUploadResponse> res = imageUploadUseCase.uploadImages(owner.getId(), trip.getId(), files);

        assertThat(res.size()).isEqualTo(1);
        assertThat(res.getFirst().uploadStatus()).isEqualTo(UploadStatus.STORED);

        imageDeleteUseCase.deleteById(owner.getId(), trip.getId(), res.getFirst().id());

        assertThatThrownBy(() -> imageService.findById(res.getFirst().id())).isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("Post 없이 이미지를 삭제할 때 다른 Post가 있는 이미지였다면 삭제할 수 없다.")
    void test05() throws IOException {
        MultipartFile[] files = new MultipartFile[] {toMultipartFile()};
        List<ImageUploadResponse> res = imageUploadUseCase.uploadImages(owner.getId(), trip.getId(), post.getId(),
            files);

        assertThat(res.size()).isEqualTo(1);
        assertThat(res.getFirst().uploadStatus()).isEqualTo(UploadStatus.STORED);

        assertThatThrownBy(() -> imageDeleteUseCase.deleteById(owner.getId(), trip.getId(), otherPost.getId(),
            res.getFirst().id())).isInstanceOf(
            ServiceException.class);
    }

    @Test
    @DisplayName("Post 없이 URL 이미지를 삭제할 때 다른 Post가 있는 이미지였다면 삭제할 수 없다.")
    void test06() throws IOException {
        MultipartFile[] files = new MultipartFile[] {toMultipartFile()};
        List<ImageUploadResponse> res = imageUploadUseCase
            .uploadImages(owner.getId(), trip.getId(), post.getId(), files);

        assertThat(res.size()).isEqualTo(1);
        assertThat(res.getFirst().uploadStatus()).isEqualTo(UploadStatus.STORED);

        assertThatThrownBy(() ->
            imageDeleteUseCase
                .deleteByUrl(owner.getId(), trip.getId(), otherPost.getId(), res.getFirst().originalFileUrl()))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("새로운 postId로 Image가 참조하는 post를 덮어쓴다.")
    void test07() throws IOException {
        MultipartFile[] files = new MultipartFile[] {toMultipartFile()};
        List<ImageUploadResponse> res = imageUploadUseCase
            .uploadImages(owner.getId(), trip.getId(), post.getId(), files);

        assertThat(res.size()).isEqualTo(1);
        assertThat(res.getFirst().uploadStatus()).isEqualTo(UploadStatus.STORED);

        assertThat(
            imageModifyUseCase
                .modifyById(owner.getId(), trip.getId(), otherPost.getId(), res.getFirst().id())
                .postId()).isNotEqualTo(post.getId());
    }

    @Test
    @DisplayName("빈 이미지 배열은 업로드 요청 경계에서 거부한다")
    void rejectEmptyImagesRequest() {
        assertThatThrownBy(() -> imageUploadUseCase.uploadImages(owner.getId(), trip.getId(), new MultipartFile[] {}))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("다중 업로드 중 빈 파일은 실패 응답으로 반환하고 나머지 파일은 저장한다")
    void uploadPartialSuccessWithEmptyFile() throws IOException {
        MultipartFile emptyFile = new MockMultipartFile(
            "images",
            "empty.jpg",
            "image/jpeg",
            new byte[] {}
        );
        MultipartFile[] files = new MultipartFile[] {emptyFile, toMultipartFile()};

        List<ImageUploadResponse> res = imageUploadUseCase.uploadImages(owner.getId(), trip.getId(), files);

        assertThat(res.size()).isEqualTo(2);
        assertThat(res.get(0).uploadStatus()).isEqualTo(UploadStatus.FAILED);
        assertThat(res.get(0).message()).isEqualTo("EMPTY_FILE");
        assertThat(res.get(1).uploadStatus()).isEqualTo(UploadStatus.STORED);
    }
}
