# Stage 1: Build stage
FROM maven:3.9.8-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies to cache them
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build package
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S sdmgroup && adduser -S sdmuser -G sdmgroup
USER sdmuser

# Copy the built jar from the build stage
COPY --from=build /app/target/sdm-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Configure Spring Profile active environment variable
ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java", "-jar", "app.jar"]
