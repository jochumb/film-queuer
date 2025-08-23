FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy gradle wrapper and gradle directory from root
COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Copy backend-specific files
COPY backend/build.gradle.kts ./backend/
COPY backend/gradle.properties ./backend/
COPY backend/src ./backend/src

RUN chmod +x ./gradlew
RUN ./gradlew :backend:build --no-daemon

EXPOSE 8080

CMD ["./gradlew", ":backend:run", "--no-daemon"]