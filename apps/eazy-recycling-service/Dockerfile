# Use a builder stage to build the JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build

# Use a lightweight JDK image to run the JAR
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]