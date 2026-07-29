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

# Render's free instance has 512MB total RAM. Without explicit limits, the
# JVM's default ergonomics can over-allocate heap for this app's dependency
# footprint (Spring, Hibernate, AWS SDK) and OOM before startup finishes.
# Serial GC has lower memory overhead than G1 and is a good fit at this size.
ENTRYPOINT ["java", "-Xmx320m", "-XX:MaxMetaspaceSize=160m", "-XX:+UseSerialGC", "-jar", "app.jar"]
