# Dev image: runs Gradle + devtools inside container
FROM eclipse-temurin:21-jdk

WORKDIR /app

# 1) Copy Gradle wrapper + config first (cache deps)
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew

# 2) Pre-warm Gradle dependency cache
RUN ./gradlew dependencies --no-daemon || true

# 3) First build needs sources in image
COPY src ./src

EXPOSE 8080

# 4) Run Spring Boot in dev mode
CMD ["./gradlew", "bootRun", "--no-daemon", "--info"]