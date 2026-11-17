# Build stage: compile the API with Maven (run from the repository root)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY brokerage-api/pom.xml brokerage-api/
COPY api-tests/pom.xml api-tests/
RUN mvn -B -q -pl brokerage-api -am dependency:go-offline
COPY brokerage-api/src brokerage-api/src
RUN mvn -B -pl brokerage-api -am package -DskipTests

# Runtime stage: only the JRE and the jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/brokerage-api/target/brokerage-api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
