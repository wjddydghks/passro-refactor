# 멀티스테이지 빌드: Gradle로 빌드하고 JRE 21 minimal로 실행
FROM gradle:8.4-jdk21 AS builder
WORKDIR /home/gradle/project
# 캐시를 최대한 활용하기 위해 의존성 관련 파일 먼저 복사
COPY build.gradle settings.gradle gradle.properties* ./
COPY gradle/ ./gradle/
COPY gradlew .
# 소스 복사
COPY src/ ./src/
# gradlew에 실행권한 부여 후 빌드 (테스트는 생략)
RUN chmod +x ./gradlew && ./gradlew --no-daemon clean bootJar -x test

# 런타임: Temurin JRE 21 이미지 사용
FROM eclipse-temurin:21-jre
WORKDIR /app
# 빌드 스테이지에서 생성된 JAR 파일 복사
COPY --from=builder /home/gradle/project/build/libs/*.jar ./app.jar
# 기본 포트 (Spring Boot 기본 포트: 8080)
EXPOSE 8080
# 필요시 JAVA_OPTS 환경변수로 추가 옵션 전달 가능
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
