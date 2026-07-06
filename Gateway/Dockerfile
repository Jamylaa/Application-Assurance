# Parent POM is at repo root — build with compose context `.` and dockerfile Gateway/Dockerfile
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY Gateway/pom.xml Gateway/
COPY Gateway/src Gateway/src
RUN mvn -B -f pom.xml install -N \
    && mvn -B -f Gateway/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/Gateway/target/gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
