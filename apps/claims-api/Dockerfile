# Build stage: compile with the pinned toolchain
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn -B -q dependency:go-offline || true
COPY src src
RUN mvn -B -q package -DskipTests

# Runtime stage: JRE only, non-root
FROM eclipse-temurin:21-jre
RUN useradd --system --home /app --shell /usr/sbin/nologin acme
WORKDIR /app
COPY --from=build /build/target/claims-api-*.jar app.jar
USER acme
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
