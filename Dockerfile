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

# Sized for Render's FREE instance: 512MB total RAM. Without explicit limits the
# JVM's default ergonomics over-allocate heap for this app's dependency footprint
# (Spring, Hibernate, AWS SDK) and OOM before startup finishes.
#
# Measured 2026-11-18 booting the packaged jar with exactly these flags: peak
# RSS 444MB, JVM committed 437MB (heap 256MB committed in full, metaspace 73MB,
# thread stacks 26MB, code cache 16MB) — so roughly 75MB of headroom on a 512MB
# instance at idle, not the ~128MB a naive heap+metaspace sum suggests. An
# earlier pass at the free tier left only ~32MB and was OOM-killed frequently.
# Serial GC has lower memory overhead than G1 and suits a single CPU.
#
# The remaining risk is concurrency, not startup: Tomcat's default max-threads
# of 200 would add up to ~100MB of stacks (200 x 512k) if they were ever all
# created. Cap server.tomcat.threads.max if real traffic arrives.
# TieredStopAtLevel=1 caps JIT at C1, which cuts code-cache/compiler memory and
# shortens the cold start that free-tier spin-down makes routine. Xss512k halves
# the default thread stack — Tomcat's 200-thread default is otherwise a real
# share of the budget.
#
# NOTE: if this is ever moved back to a 2GB Standard instance, raise these
# (1024m/256m was the tuned Standard setting) — 256m heap will throttle there.
ENTRYPOINT ["java", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Xss512k", "-jar", "app.jar"]
