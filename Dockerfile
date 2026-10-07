# 1단계: 빌드 (JDK)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성만 먼저 받아서 레이어 캐시 (소스만 바뀌면 이 단계는 재사용)
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null

COPY src src
# 테스트는 CI 앞 단계에서 실행하므로 이미지 빌드에서는 생략
RUN ./gradlew --no-daemon bootJar -x test

# 2단계: 실행 (JRE)
FROM eclipse-temurin:21-jre
WORKDIR /app

# k8s runAsNonRoot 를 통과하려면 숫자 UID 로 지정해야 한다.
# (Ubuntu 기반 이미지에는 uid 1000 'ubuntu' 사용자가 이미 있어 10001 을 쓴다)
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

COPY --from=build --chown=10001:10001 /workspace/build/libs/*.jar /app/app.jar

USER 10001
EXPOSE 8080

# exec 형식이라 java 가 PID 1 로 SIGTERM 을 직접 받아 graceful shutdown 한다.
# MaxRAMPercentage: Pod memory limit(1Gi)에 맞춰 힙 크기를 정한다.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]