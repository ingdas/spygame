# Build stage
FROM gradle:8.6-jdk21 AS builder

# Copy the project files
WORKDIR /app
COPY . .

# Install Node.js for frontend build
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs

# Build the application and create the fat JAR
RUN gradle fatJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /app/build/libs/*-fat.jar app.jar

# Expose the port your application runs on (adjust if needed)
EXPOSE 8089

# Command to run the application
CMD ["java", "-jar", "app.jar"]