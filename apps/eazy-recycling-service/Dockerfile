FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

ARG JAR_FILE=./build/libs/app.jar
COPY ${JAR_FILE} /app/app.jar


WORKDIR /app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]