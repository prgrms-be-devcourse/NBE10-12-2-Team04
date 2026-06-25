package com.triptrace.global.initData;

import lombok.SneakyThrows;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

@Profile("dev")
@Configuration
public class DevInitData {
    @Bean
    ApplicationRunner devInitDataApplicationRunner() {
        return args -> {
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
