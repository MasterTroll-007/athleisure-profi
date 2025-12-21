FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/fitness-backend.jar ./app.jar
RUN mkdir -p /app/uploads/plans /app/uploads/images
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
