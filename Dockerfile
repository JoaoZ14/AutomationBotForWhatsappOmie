# Estágio 1: build (Maven + JDK 17, Alpine)
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# Estágio 2: execução
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# JAR executável (não usar *: existe também *-plain.jar)
COPY --from=build /app/target/automation-bot-0.0.1-SNAPSHOT.jar app.jar

# Render define PORT; EXPOSE só documenta
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
