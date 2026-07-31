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

# Render's Standard instance has 2GB total RAM. Without explicit limits, the
# JVM's default ergonomics can over-allocate heap for this app's dependency
# footprint (Spring, Hibernate, AWS SDK) and OOM before startup finishes.
# Heap+metaspace are capped well under the container limit so threads, native
# buffers (AWS SDK client, JDBC driver), and OS overhead have real headroom —
# on the previous 512MB free tier this margin was ~32MB, which caused frequent
# OOM kills; at 2GB it's roughly 700MB. Serial GC has lower memory overhead
# than G1 and is a good fit for a single-CPU instance.
ENTRYPOINT ["java", "-Xmx1024m", "-XX:MaxMetaspaceSize=256m", "-XX:+UseSerialGC", "-jar", "app.jar"]
