package com.triptrace.domain.image.image.facade;

import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.module.ImageProcessor;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
    @Autowired
    private ImageProcessor imageProcessor;
    byte[] bytes;
    @Autowired
    private ImageService imageService;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("custom.servingImage", () -> tempDir.resolve("serving").toString());
        registry.add("custom.thumbnailImage", () -> tempDir.resolve("thumbnail").toString());
    }
    @BeforeEach
    public void setUp() {
        imageProcessor = new ImageProcessor(
            tempDir.resolve("serving").toString(),
            tempDir.resolve("thumbnail").toString()
        );
        try {
            bytes = getClass().getResourceAsStream(imageFileName).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Member member =
            createMember("ssssssssssssssssusername");
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
        return memberRepository.save(new Member(
            "%s@test.com".formatted(username),
            username,
            "password1234",
            UUID.randomUUID().toString(),
            "imageUrl",
            MemberStatus.ACTIVE
        ));
    }

    private Trip createTrip(Member member){
        Trip trip = tripRepository.save(new Trip(
            member,
            "교토 여행",
            "일본",
            "교토",
            LocalDateTime.of(2024, 4, 1, 0, 0),
            LocalDateTime.of(2024, 4, 5, 0, 0),
            true
        ));
        return trip;
    }
    @Test
    @DisplayName("upload")
    void test01() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        MultipartFile multipartFile = new MockMultipartFile(
            "images",
            imageFileName,
            "image/jpeg",
            byteArrayInputStream
        );
        MultipartFile[] files = new MultipartFile[]{multipartFile};
        List<ImageUploadResponse> res = imageUploadFacade.uploadImages(owner.getId(), trip.getId(), files);

        assertThat(res.size()).isEqualTo(1);
        assertThat(res.getFirst().uploadStatus()).isEqualTo(UploadStatus.STORED);
        System.out.println(res.getFirst());
        ImageServiceResponse imageServiceResponse = imageService.findById(res.getFirst().id());
        System.out.println(imageServiceResponse);
    }



}
