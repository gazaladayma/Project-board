# ===== Build Stage =====
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ===== Runtime Stage =====
FROM eclipse-temurin:21-jre-alpine

# Apply current Alpine security updates
RUN apk update && apk upgrade --no-cache

WORKDIR /app

# Create a dedicated non-root user and group
RUN addgroup -S -g 10001 appgroup \
    && adduser -S -D -H -u 10001 -G appgroup appuser

COPY --from=build --chown=10001:10001 /app/target/*.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

# Check the application's HTTP endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
