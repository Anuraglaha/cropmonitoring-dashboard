# Stage 1: Build the application
FROM eclipse-temurin:20-jdk AS build
WORKDIR /app
# Copy the gradle configuration files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
# Copy the source code
COPY src ./src
# Fix permissions for the gradlew wrapper
RUN chmod +x ./gradlew
# Build the application (skipping tests for faster deployment)
RUN ./gradlew build -x test

# Stage 2: Run the application
FROM eclipse-temurin:20-jre
WORKDIR /app
# Copy the built jar file from the build stage
COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar
# Expose port 8080
EXPOSE 8080
# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
