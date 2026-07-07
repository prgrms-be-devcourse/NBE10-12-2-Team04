package com.triptrace.global.initData;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.entity.MarkerSource;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.entity.TripLike;
import com.triptrace.domain.trip.tripLike.repository.TripLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;

@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class DevInitData {

    private final MemberRepository memberRepository;
    private final TripRepository tripRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final MarkerRepository markerRepository;
    private final ImageRepository imageRepository;
    private final TripLikeRepository tripLikeRepository;

    @Bean
    ApplicationRunner devInitDataApplicationRunner() {
        return args -> {
            if (memberRepository.existsByEmail("user1@test.com")) return;

            // MEMBER
            List<Member> members = createMembers();
            Member user1 = members.get(0);
            Member user2 = members.get(1);

            // TRIPS
            List<Trip> trips = createTrips(user1, user2);

            // POSTS
            Map<String, Post> posts = createPosts(trips);

            // IMAGES
            Map<String, Image> images = createImages(user1, user2, posts);

            // TRIP 대표 이미지
            updateTripImages(trips, images);

            // MARKERS
            createMarkers(posts, images);

            // LIKES
            createLikes(user1, user2, trips);


            printSeedInfo(trips);
        };
    }


    // MEMBER
    private List<Member> createMembers() {
        Member user1 = memberRepository.save(new Member(
            "user1@test.com",
            "user1",
            passwordEncoder.encode("password1234"),
            "https://example.com/profiles/user1.png",
            MemberStatus.ACTIVE
        ));

        Member user2 = memberRepository.save(new Member(
            "user2@test.com",
            "user2",
            passwordEncoder.encode("password1234"),
            "https://example.com/profiles/user2.png",
            MemberStatus.ACTIVE
        ));

        return List.of(user1, user2);
    }


    // TRIP
    private List<Trip> createTrips(Member user1, Member user2) {

        Trip t1 = tripRepository.save(new Trip(
            user1, "user1 공개 교토 여행",
            "일본", "교토",
            LocalDateTime.of(2026,1,1,0,0),
            LocalDateTime.of(2026,1,5,0,0),
            true
        ));

        Trip t2 = tripRepository.save(new Trip(
            user1, "user1 비공개 서울 여행",
            "한국", "서울",
            LocalDateTime.of(2026,2,1,0,0),
            LocalDateTime.of(2026,2,3,0,0),
            false
        ));

        Trip t3 = tripRepository.save(new Trip(
            user2, "user2 공개 부산 여행",
            "한국", "부산",
            LocalDateTime.of(2026,3,10,0,0),
            LocalDateTime.of(2026,3,12,0,0),
            true
        ));

        return List.of(t1, t2, t3);
    }


    // POST
    private Map<String, Post> createPosts(List<Trip> trips) {

        Post p1 = postRepository.save(new Post(trips.get(0),
            LocalDate.of(2026,1,1),
            "교토 첫째 날",
            "기온 거리와 청수사를 둘러봤습니다."
        ));

        Post p2 = postRepository.save(new Post(trips.get(0),
            LocalDate.of(2026,1,2),
            "교토 둘째 날",
            "아라시야마와 카페를 방문했습니다."
        ));

        Post p3 = postRepository.save(new Post(trips.get(1),
            LocalDate.of(2026,2,1),
            "서울 비공개 기록",
            "개인 일정 메모입니다."
        ));

        Post p4 = postRepository.save(new Post(trips.get(2),
            LocalDate.of(2026,3,10),
            "부산 첫째 날",
            "해운대와 광안리를 걸었습니다."
        ));

        return Map.of(
            "p1", p1,
            "p2", p2,
            "p3", p3,
            "p4", p4
        );
    }


    // IMAGE

    private Map<String, Image> createImages(Member user1, Member user2, Map<String, Post> posts) {

        Image i1 = imageRepository.save(new Image(
            user1, posts.get("p1").getTrip(), posts.get("p1"),
            "https://example.com/images/kyoto1.jpg",
            "https://example.com/images/thumbs/kyoto1.jpg",
            1000L, "image/jpeg", UploadStatus.STORED
        ));

        Image i2 = imageRepository.save(new Image(
            user1, posts.get("p2").getTrip(), posts.get("p2"),
            "https://example.com/images/kyoto2.jpg",
            "https://example.com/images/thumbs/kyoto2.jpg",
            1200L, "image/jpeg", UploadStatus.STORED
        ));

        Image i3 = imageRepository.save(new Image(
            user1, posts.get("p3").getTrip(), posts.get("p3"),
            "https://example.com/images/seoul1.jpg",
            "https://example.com/images/thumbs/seoul1.jpg",
            900L, "image/jpeg", UploadStatus.STORED
        ));

        Image i4 = imageRepository.save(new Image(
            user2, posts.get("p4").getTrip(), posts.get("p4"),
            "https://example.com/images/busan1.jpg",
            "https://example.com/images/thumbs/busan1.jpg",
            1500L, "image/jpeg", UploadStatus.STORED
        ));

        return Map.of(
            "i1", i1,
            "i2", i2,
            "i3", i3,
            "i4", i4
        );
    }


    // TRIP IMAGE

    private void updateTripImages(List<Trip> trips, Map<String, Image> images) {
        trips.get(0).changeRepresentativeImage(images.get("i1"));
        trips.get(1).changeRepresentativeImage(images.get("i3"));
        trips.get(2).changeRepresentativeImage(images.get("i4"));

        tripRepository.saveAll(trips);
    }


    // MARKER

    private void createMarkers(Map<String, Post> posts, Map<String, Image> images) {

        markerRepository.save(new Marker(
            posts.get("p1"),
            new BigDecimal("35.011564"),
            new BigDecimal("135.768149"),
            "기온",
            LocalDateTime.of(2026,1,1,10,0),
            MarkerSource.MANUAL,
            images.get("i1")
        ));

        markerRepository.save(new Marker(
            posts.get("p2"),
            new BigDecimal("35.017000"),
            new BigDecimal("135.671000"),
            "아라시야마",
            LocalDateTime.of(2026,1,2,11,0),
            MarkerSource.MANUAL,
            images.get("i2")
        ));

        markerRepository.save(new Marker(
            posts.get("p3"),
            new BigDecimal("37.566500"),
            new BigDecimal("126.978000"),
            "광화문",
            LocalDateTime.of(2026,2,1,14,0),
            MarkerSource.MANUAL,
            images.get("i3")
        ));

        markerRepository.save(new Marker(
            posts.get("p4"),
            new BigDecimal("35.158698"),
            new BigDecimal("129.160384"),
            "해운대",
            LocalDateTime.of(2026,3,10,15,0),
            MarkerSource.MANUAL,
            images.get("i4")
        ));
    }


    // LIKE

    private void createLikes(Member user1, Member user2, List<Trip> trips) {

        tripLikeRepository.save(new TripLike(user2, trips.get(0)));
        trips.get(0).increaseLikeCount();

        tripLikeRepository.save(new TripLike(user1, trips.get(2)));
        trips.get(2).increaseLikeCount();

        tripRepository.saveAll(trips);
    }


    // PRINT

    private void printSeedInfo(List<Trip> trips) {
        System.out.println("========== SEED DATA READY ==========");
        System.out.println("user1@test.com / user2@test.com");

        System.out.println("TRIPS:");
        System.out.println("user1PublicTrip = " + trips.get(0).getId());
        System.out.println("user1PrivateTrip = " + trips.get(1).getId());
        System.out.println("user2PublicTrip = " + trips.get(2).getId());

        System.out.println("=====================================");
    }
}


//            cmd.runAsync(
//                "npx{{DOT_CMD}}",
//                "--yes",
//                "--package", "typescript@v5",
//                "--package", "openapi-typescript",
//                "openapi-typescript", "http://localhost:8080/v3/api-docs/apiV1",
//                "-o", "../front/src/lib/backend/apiV1/schema.d.ts"
//            );


/*
    public static class cmd {
        @SneakyThrows
        public static void run(String... args) {
            boolean isWindows = System
                .getProperty("os.name")
                .toLowerCase()
                .contains("win");

            ProcessBuilder builder = new ProcessBuilder(
                Arrays.stream(args)
                    .map(arg -> arg.replace("{{DOT_CMD}}", isWindows ? ".cmd" : ""))
                    .toArray(String[]::new)
            );

            // 에러 스트림도 출력 스트림과 함께 병합
            builder.redirectErrorStream(true);

            // 프로세스 시작
            Process process = builder.start();

            // 결과 출력
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line); // 결과 한 줄씩 출력
                }
            }

            // 종료 코드 확인
            int exitCode = process.waitFor();
            System.out.println("종료 코드: " + exitCode);
        }

        public static void runAsync(String... args) {
            new Thread(() -> {
                run(args);
            }).start();
        }
    }
}
*/
