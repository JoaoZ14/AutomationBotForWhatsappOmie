FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/target/automation-bot-0.0.1-SNAPSHOT.jar .

EXPOSE 8080

# preferIPv4Stack: evita "Network is unreachable" em alguns hosts (ex.: Render → Supabase) quando o Java tenta IPv6 primeiro.
CMD ["java", "-Djava.net.preferIPv4Stack=true", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=70.0", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/./urandom", "-jar", "automation-bot-0.0.1-SNAPSHOT.jar"]
