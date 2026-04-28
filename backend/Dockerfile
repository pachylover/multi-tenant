FROM gradle:8.10.2-jdk21 AS build
WORKDIR /home/gradle/project

COPY build.gradle settings.gradle ./
COPY src src

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080 9092
ENTRYPOINT ["java","-jar","/app/app.jar"]

