# Use a lightweight JRE image to run the JAR
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/app.jar app.jar
ENTRYPOINT ["/bin/sh", "-c", "umask 077 && printf \"%s\" \"$CLIENT_PFX_B64\" | base64 -d > /tmp/client.pfx && chmod 600 /tmp/client.pfx && exec java -jar app.jar"]
