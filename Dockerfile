FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .

# FIX: give execute permission to gradlew
RUN chmod +x gradlew

RUN ./gradlew build -x test

EXPOSE 8080

CMD ["java", "-jar", "build/libs/*.jar"]
