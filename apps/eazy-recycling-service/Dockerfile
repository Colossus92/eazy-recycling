FROM gradle:jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Poort instellen en applicatie starten
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]