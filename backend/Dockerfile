# syntax=docker/dockerfile:1
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
RUN mkdir -p /srv/personal-web/uploads /srv/personal-web/repos /etc/personal-web/keys
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx384m", "-Xms128m", "-jar", "/app/app.jar"]
