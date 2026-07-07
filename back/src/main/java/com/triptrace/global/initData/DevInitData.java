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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class DevInitData {

    private static final int MEMBER_COUNT = 10;

    private final MemberRepository memberRepository;
    private final TripRepository tripRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final MarkerRepository markerRepository;
    private final ImageRepository imageRepository;
    private final TripLikeRepository tripLikeRepository;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner devInitDataApplicationRunner() {
        return args -> {
            relaxMarkerCoordinateColumns();

            if (memberRepository.existsByEmail("user1@test.com")) return;

            // MEMBER
            List<Member> members = createMembers();

            // TRIPS
            List<Trip> trips = createTrips(members);

            // POSTS + IMAGES + MARKERS (게시글 스펙 하나에 좌표/장소/이미지를 묶어 함께 생성)
            createContent(members, trips, postSpecs());

            // LIKES (여행기마다 좋아요 수를 다양하게)
            createLikes(members, trips);

            printSeedInfo(trips);
        };
    }

    private void relaxMarkerCoordinateColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE marker ALTER COLUMN center_lat DROP NOT NULL");
            jdbcTemplate.execute("ALTER TABLE marker ALTER COLUMN center_lng DROP NOT NULL");
        } catch (Exception ignored) {
            // Fresh schemas already follow the entity mapping, and other DBs may not need this dev-only patch.
        }
    }


    // MEMBER
    private List<Member> createMembers() {

        List<Member> members = new ArrayList<>();

        for (int i = 1; i <= MEMBER_COUNT; i++) {
            members.add(memberRepository.save(
                new Member(
                    "user" + i + "@test.com",
                    "user" + i,
                    passwordEncoder.encode("password1234"),
                    "https://example.com/profiles/user" + i + ".png",
                    MemberStatus.ACTIVE
                )
            ));
        }

        return members;
    }


    // TRIP
    private List<Trip> createTrips(List<Member> members) {

        String[][] tripInfos = {
            {"user1 공개 교토 여행", "일본", "교토"},
            {"user1 비공개 서울 여행", "한국", "서울"},
            {"user2 공개 부산 여행", "한국", "부산"},
            {"user2 강릉 여행", "한국", "강릉"},
            {"user3 제주 여행", "한국", "제주"},
            {"user3 오사카 여행", "일본", "오사카"},
            {"user4 도쿄 여행", "일본", "도쿄"},
            {"user4 타이베이 여행", "대만", "타이베이"},
            {"user5 다낭 여행", "베트남", "다낭"},
            {"user5 방콕 여행", "태국", "방콕"},
            {"user6 홍콩 여행", "홍콩", "홍콩"},
            {"user6 싱가포르 여행", "싱가포르", "싱가포르"},
            {"user7 여수 여행", "한국", "여수"},
            {"user7 후쿠오카 여행", "일본", "후쿠오카"},
            {"user8 파리 여행", "프랑스", "파리"},
            {"user8 로마 여행", "이탈리아", "로마"},
            {"user9 뉴욕 여행", "미국", "뉴욕"},
            {"user9 런던 여행", "영국", "런던"},
            {"user10 시드니 여행", "호주", "시드니"},
            {"user10 발리 여행", "인도네시아", "발리"}
        };

        boolean[] visibility = {
            true, false,
            true, true,
            true, false,
            true, true,
            true, false,
            true, true,
            true, false,
            true, true,
            true, false,
            true, true
        };

        List<Trip> trips = new ArrayList<>();

        for (int i = 0; i < tripInfos.length; i++) {

            Member owner = members.get(i / 2);  // 멤버 1명이 여행 2개씩 소유
            int month = (i % 12) + 1;

            trips.add(
                tripRepository.save(
                    new Trip(
                        owner,
                        tripInfos[i][0],
                        tripInfos[i][1],
                        tripInfos[i][2],
                        LocalDateTime.of(2026, month, 1, 0, 0),
                        LocalDateTime.of(2026, month, 4, 0, 0),
                        visibility[i]
                    )
                )
            );
        }

        return trips;
    }


    // POST 스펙 (게시글 + 이미지 + 마커를 함께 만들기 위한 데이터)
    private record PostSpec(
        int tripIndex,
        LocalDate date,
        String title,
        String memo,
        String placeName,
        String lat,
        String lng,
        String imageSlug
    ) {}

    private List<PostSpec> postSpecs() {
        return List.of(
            new PostSpec(0, LocalDate.of(2026, 1, 1), "교토 첫째 날", "기온 거리와 청수사를 둘러봤습니다.", "기온", "35.0037", "135.7750", "kyoto1"),
            new PostSpec(0, LocalDate.of(2026, 1, 2), "교토 둘째 날", "아라시야마 대나무숲을 걸었습니다.", "아라시야마", "35.0170", "135.6710", "kyoto2"),

            new PostSpec(1, LocalDate.of(2026, 2, 1), "서울 비공개 기록", "광화문과 경복궁을 산책했습니다.", "광화문", "37.5759", "126.9769", "seoul1"),

            new PostSpec(2, LocalDate.of(2026, 3, 1), "부산 첫째 날", "해운대 바다를 걸었습니다.", "해운대", "35.1587", "129.1604", "busan1"),
            new PostSpec(2, LocalDate.of(2026, 3, 2), "부산 둘째 날", "광안리 야경을 봤습니다.", "광안리", "35.1532", "129.1187", "busan2"),

            new PostSpec(3, LocalDate.of(2026, 4, 1), "강릉 여행", "경포대에서 커피를 마셨습니다.", "경포대", "37.7955", "128.8962", "gangneung1"),

            new PostSpec(4, LocalDate.of(2026, 5, 1), "제주 첫째 날", "성산일출봉에 올랐습니다.", "성산일출봉", "33.4586", "126.9425", "jeju1"),
            new PostSpec(4, LocalDate.of(2026, 5, 2), "제주 둘째 날", "협재해변에서 쉬었습니다.", "협재해변", "33.3948", "126.2396", "jeju2"),

            new PostSpec(5, LocalDate.of(2026, 6, 1), "오사카 기록", "도톤보리에서 먹거리를 즐겼습니다.", "도톤보리", "34.6687", "135.5013", "osaka1"),

            new PostSpec(6, LocalDate.of(2026, 7, 1), "도쿄 첫째 날", "시부야 스크램블을 건넜습니다.", "시부야", "35.6595", "139.7005", "tokyo1"),
            new PostSpec(6, LocalDate.of(2026, 7, 2), "도쿄 둘째 날", "아사쿠사 센소지를 방문했습니다.", "아사쿠사", "35.7148", "139.7967", "tokyo2"),

            new PostSpec(7, LocalDate.of(2026, 8, 1), "타이베이 여행", "타이베이101 전망대에 올랐습니다.", "타이베이101", "25.0339", "121.5645", "taipei1"),

            new PostSpec(8, LocalDate.of(2026, 9, 1), "다낭 여행", "미케 비치에서 휴양했습니다.", "미케비치", "16.0600", "108.2470", "danang1"),

            new PostSpec(9, LocalDate.of(2026, 10, 1), "방콕 기록", "왓포 사원을 둘러봤습니다.", "왓포", "13.7465", "100.4927", "bangkok1"),

            new PostSpec(10, LocalDate.of(2026, 11, 1), "홍콩 여행", "빅토리아 피크에서 야경을 봤습니다.", "빅토리아피크", "22.2759", "114.1455", "hongkong1"),

            new PostSpec(11, LocalDate.of(2026, 12, 1), "싱가포르 첫째 날", "마리나베이를 걸었습니다.", "마리나베이", "1.2834", "103.8607", "singapore1"),
            new PostSpec(11, LocalDate.of(2026, 12, 2), "싱가포르 둘째 날", "가든스 바이 더 베이를 봤습니다.", "가든스바이더베이", "1.2816", "103.8636", "singapore2"),

            new PostSpec(12, LocalDate.of(2026, 1, 3), "여수 여행", "돌산공원에서 바다를 봤습니다.", "돌산공원", "34.7362", "127.7628", "yeosu1"),

            new PostSpec(13, LocalDate.of(2026, 2, 3), "후쿠오카 기록", "다자이후를 방문했습니다.", "다자이후", "33.5209", "130.5350", "fukuoka1"),

            new PostSpec(14, LocalDate.of(2026, 3, 3), "파리 첫째 날", "에펠탑을 봤습니다.", "에펠탑", "48.8584", "2.2945", "paris1"),
            new PostSpec(14, LocalDate.of(2026, 3, 4), "파리 둘째 날", "루브르 박물관을 관람했습니다.", "루브르", "48.8606", "2.3376", "paris2"),

            new PostSpec(15, LocalDate.of(2026, 4, 3), "로마 여행", "콜로세움을 둘러봤습니다.", "콜로세움", "41.8902", "12.4922", "rome1"),

            new PostSpec(16, LocalDate.of(2026, 5, 3), "뉴욕 여행", "타임스퀘어를 걸었습니다.", "타임스퀘어", "40.7580", "-73.9855", "newyork1"),

            new PostSpec(17, LocalDate.of(2026, 6, 3), "런던 기록", "빅벤 앞에서 사진을 찍었습니다.", "빅벤", "51.5007", "-0.1246", "london1"),

            new PostSpec(18, LocalDate.of(2026, 7, 3), "시드니 여행", "오페라하우스를 봤습니다.", "오페라하우스", "-33.8568", "151.2153", "sydney1"),

            new PostSpec(19, LocalDate.of(2026, 8, 3), "발리 여행", "우붓에서 휴양했습니다.", "우붓", "-8.5069", "115.2625", "bali1")
        );
    }


    // POST + IMAGE + MARKER + 대표 이미지
    private void createContent(List<Member> members, List<Trip> trips, List<PostSpec> specs) {

        // 여행기별 대표 이미지(각 여행기의 첫 이미지)를 담아둔다
        Map<Integer, Image> representativeImages = new HashMap<>();

        for (PostSpec spec : specs) {

            Trip trip = trips.get(spec.tripIndex());
            Member owner = trip.getOwner();

            // POST
            Post post = postRepository.save(new Post(
                trip,
                spec.date(),
                spec.title(),
                spec.memo()
            ));

            // IMAGE
            Image image = imageRepository.save(new Image(
                owner,
                trip,
                post,
                "https://example.com/images/" + spec.imageSlug() + ".jpg",
                "https://example.com/images/thumbs/" + spec.imageSlug() + ".jpg",
                1024L,
                "image/jpeg",
                UploadStatus.STORED
            ));
            representativeImages.putIfAbsent(spec.tripIndex(), image);

            // MARKER (방문 시각은 #79 규칙대로 게시글 날짜에 맞춘다)
            markerRepository.save(new Marker(
                post,
                new BigDecimal(spec.lat()),
                new BigDecimal(spec.lng()),
                spec.placeName(),
                spec.date().atTime(10, 0),
                MarkerSource.MANUAL,
                image
            ));
        }

        // TRIP 대표 이미지
        representativeImages.forEach((tripIndex, image) ->
            trips.get(tripIndex).changeRepresentativeImage(image)
        );
        tripRepository.saveAll(trips);
    }


    // LIKE (여행기마다 좋아요 수를 다양하게. 비공개 여행은 0)
    private void createLikes(List<Member> members, List<Trip> trips) {

        // 여행기 인덱스별 목표 좋아요 수
        int[] likeCounts = {
            8, 0,   // 교토 / 서울(비공개)
            3, 6,   // 부산 / 강릉
            9, 0,   // 제주 / 오사카(비공개)
            2, 5,   // 도쿄 / 타이베이
            7, 0,   // 다낭 / 방콕(비공개)
            4, 1,   // 홍콩 / 싱가포르
            6, 0,   // 여수 / 후쿠오카(비공개)
            9, 3,   // 파리 / 로마
            8, 0,   // 뉴욕 / 런던(비공개)
            5, 2    // 시드니 / 발리
        };

        int n = members.size();

        for (int t = 0; t < trips.size(); t++) {

            Trip trip = trips.get(t);
            int target = likeCounts[t];
            int added = 0;

            // 여행기마다 좋아요를 누르는 멤버가 겹치지 않도록 시작 지점을 회전시킨다
            for (int k = 0; added < target && k < n; k++) {

                Member liker = members.get((t + k) % n);
                if (liker == trip.getOwner()) continue;  // 본인 여행에는 좋아요 x

                tripLikeRepository.save(new TripLike(liker, trip));
                trip.increaseLikeCount();
                added++;
            }
        }

        tripRepository.saveAll(trips);
    }


    // PRINT
    private void printSeedInfo(List<Trip> trips) {
        System.out.println("========== SEED DATA READY ==========");
        System.out.println("members: user1@test.com ~ user" + MEMBER_COUNT + "@test.com / password1234");
        System.out.println("TRIPS & LIKES:");

        for (Trip trip : trips) {
            System.out.printf(
                "  [%d] %s (공개=%b) likes=%d%n",
                trip.getId(),
                trip.getTitle(),
                trip.isVisibility(),
                trip.getLikeCount()
            );
        }

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
