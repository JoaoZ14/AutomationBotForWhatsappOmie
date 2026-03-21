# Build
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

# Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /app/target/automation-bot-*.jar app.jar
USER spring:spring
EXPOSE 8080
# Render (IPv4) + Supabase direct (às vezes só IPv6): ajuda se o DNS tiver A e AAAA; se ainda falhar,
# use no painel Supabase a URI do pooler (Session) ou o add-on IPv4 — ver application.yml.
ENV JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"
ENTRYPOINT ["java", "-jar", "app.jar"]
