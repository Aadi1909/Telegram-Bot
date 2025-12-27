# -------- BUILD STAGE --------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar

# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# COPY JAR FROM BUILDER STAGE
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
