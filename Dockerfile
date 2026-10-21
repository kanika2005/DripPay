# Render / Docker build from repository root (backend lives in backend/)
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY backend/mvnw backend/.mvn backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY backend/src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --system --uid 10001 appuser
USER appuser

COPY --from=build /app/target/drippay-backend-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
