FROM gradle:8.6-jdk21 AS builder

WORKDIR /app
COPY . .

RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs

RUN gradle fatJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY --from=builder /app/build/libs/*-fat.jar app.jar

# Use PORT environment variable
EXPOSE ${PORT}

CMD ["sh", "-c", "java -jar app.jar"]