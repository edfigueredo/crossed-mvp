FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean verify
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && useradd --system --create-home crossed
COPY --from=build --chown=crossed:crossed /app/target/crossed-1.0.0.jar app.jar
USER crossed
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
