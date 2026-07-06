# Build JAR inside image (no local `mvn package` required)
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/GestionProduit-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9093
ENTRYPOINT ["java", "-jar", "app.jar"]
