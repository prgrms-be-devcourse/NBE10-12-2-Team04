package com.triptrace.global.initData;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
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

@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class DevInitData {
    private final MemberRepository memberRepository;
    private final TripRepository tripRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner devInitDataApplicationRunner() {
        return args -> {
            if (memberRepository.existsByEmail("user1@test.com")) {
                return;
            }

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

            Trip user1PublicTrip = tripRepository.save(new Trip(
                user1,
                "user1 공개 교토 여행",
                "일본",
                "교토",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 5, 0, 0),
                true
            ));

            Trip user1PrivateTrip = tripRepository.save(new Trip(
                user1,
                "user1 비공개 서울 여행",
                "한국",
                "서울",
                LocalDateTime.of(2026, 2, 1, 0, 0),
                LocalDateTime.of(2026, 2, 3, 0, 0),
                false
            ));

            Trip user2PublicTrip = tripRepository.save(new Trip(
                user2,
                "user2 공개 부산 여행",
                "한국",
                "부산",
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(2026, 3, 12, 0, 0),
                true
            ));

            postRepository.save(new Post(
                user1PublicTrip,
                LocalDate.of(2026, 1, 1),
                "교토 첫째 날",
                "기온 거리와 청수사를 둘러봤습니다."
            ));

            postRepository.save(new Post(
                user1PublicTrip,
                LocalDate.of(2026, 1, 2),
                "교토 둘째 날",
                "아라시야마와 카페를 방문했습니다."
            ));

            postRepository.save(new Post(
                user1PrivateTrip,
                LocalDate.of(2026, 2, 1),
                "서울 비공개 기록",
                "개인 일정 메모입니다."
            ));

            postRepository.save(new Post(
                user2PublicTrip,
                LocalDate.of(2026, 3, 10),
                "부산 첫째 날",
                "해운대와 광안리를 걸었습니다."
            ));

//            cmd.runAsync(
//                "npx{{DOT_CMD}}",
//                "--yes",
//                "--package", "typescript@v5",
//                "--package", "openapi-typescript",
//                "openapi-typescript", "http://localhost:8080/v3/api-docs/apiV1",
//                "-o", "../front/src/lib/backend/apiV1/schema.d.ts"
//            );
        };
    }

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
