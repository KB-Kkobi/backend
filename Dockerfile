FROM eclipse-temurin:17-jdk-noble AS build

WORKDIR /app

COPY gradle gradle
COPY gradlew gradlew.bat build.gradle settings.gradle ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew clean war -x test -x installGitHooks --no-daemon

FROM tomcat:9.0.120-jre17-temurin-noble

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
