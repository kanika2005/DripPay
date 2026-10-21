# Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -q -DskipTests package

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --system --uid 10001 appuser
USER appuser

COPY --from=build /app/target/drippay-backend-*.jar /app/app.jar

# Render sets PORT; Spring reads server.port from PORT or SERVER_PORT (see application.yml)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
