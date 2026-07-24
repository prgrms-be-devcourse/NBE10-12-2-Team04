#공식 오픈 jdk
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre-noble AS runner

LABEL project="trip-trace-be1204"
LABEL version="1.0"

WORKDIR /app

ARG LOG_DIR=$LOG_DIR
ARG IMAGE_UPLOAD=$IMAGE_UPLOAD

RUN addgroup --system spring && adduser --system --ingroup spring spring
RUN mkdir -p ${LOG_DIR} ${IMAGE_UPLOAD} && chown -R spring:spring ${LOG_DIR} ${IMAGE_UPLOAD}

COPY --from=builder  --chown=spring:spring /app/build/libs/*.jar app.jar

USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
