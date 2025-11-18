### --- 1단계: 빌드 단계 --- ###
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Gradle Wrapper 및 프로젝트 설정 파일 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Gradle 의존성 캐싱(소스코드 제외)
RUN ./gradlew dependencies --no-daemon || true

# 전체 소스 복사
COPY . .

# JAR 빌드
RUN ./gradlew clean bootJar --no-daemon


### --- 2단계: 실행 단계 (JRE) --- ###
FROM eclipse-temurin:21-jre

WORKDIR /app

# 보안 강화를 위해 non-root user 생성
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser

# builder 스테이지에서 빌드된 jar 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
