# Etape 1 : compilation
FROM docker.io/library/maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Les dependances sont mises en cache tant que le pom ne change pas.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# Etape 2 : execution
FROM docker.io/library/eclipse-temurin:21-jre
WORKDIR /app

# curl sert au healthcheck de docker compose.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --create-home app

COPY --from=build /build/target/*.jar app.jar
USER app

EXPOSE 8081
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/app/app.jar"]
