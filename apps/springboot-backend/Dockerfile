# Use a lightweight JRE image to run the JAR
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/app.jar app.jar
ENTRYPOINT ["/bin/sh", "-c", "\
  echo '[DEBUG] Starting container...' && \
  echo '[DEBUG] Working dir:' $(pwd) && \
  echo '[DEBUG] Environment variable length (CLIENT_PFX_B64):' $(echo -n \"$CLIENT_PFX_B64\" | wc -c) && \
  umask 077 && \
  printf '%s' \"$CLIENT_PFX_B64\" | base64 -d > /tmp/client.pfx && \
  echo '[DEBUG] Written /tmp/client.pfx' && \
  ls -l /tmp/client.pfx && \
  echo '[DEBUG] File size:' $(stat -c%s /tmp/client.pfx 2>/dev/null || stat -f%z /tmp/client.pfx) && \
  echo '[DEBUG] Launching application...' && \
  exec java -jar app.jar \
"]
