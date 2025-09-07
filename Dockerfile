FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies || true
COPY src/ src/
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:17-jre

RUN useradd -ms /bin/bash appuser
USER appuser
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java","-jar","/app/app.jar"]
