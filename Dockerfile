# 1단계: Java 17 환경에서 WAR 빌드
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew clean war -x test -x installGitHooks --no-daemon


# 2단계: Tomcat 9에서 WAR 실행
FROM tomcat:9-jdk17-temurin

# Tomcat 기본 애플리케이션 제거
RUN rm -rf /usr/local/tomcat/webapps/*

# 빌드된 WAR를 ROOT.war로 배포
COPY --from=builder /app/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]