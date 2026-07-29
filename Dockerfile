# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# --- Run stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# CockroachDB Cloud requires sslmode=verify-full, which needs this CA root cert
# (public, non-secret — ISRG Root X1) present on the filesystem at runtime.
COPY docker/cockroachdb-root.crt /root/.postgresql/root.crt

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
