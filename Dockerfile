FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew installDist --no-daemon
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/build/install/EcoAPI /app
RUN groupadd --system eco && useradd --system --gid eco eco
USER eco
EXPOSE 8080
ENTRYPOINT ["/app/bin/EcoAPI"]
