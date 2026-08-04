# ============================================================================
# TICKET-ADV146 — Spring Boot Dockerfile (multi-stage Maven -> JRE 25 slim)
#
# Stage 1: build the JAR with Temurin JDK 25 + Maven.
# Stage 2: copy ONLY the JAR onto a slim JRE image.
# ============================================================================

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN JAR_FILE=$(ls target/reconx-*.jar 2>/dev/null | grep -v '\.original$' | head -n 1); \
    if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then \
        cp "$JAR_FILE" app.jar; \
    else \
        ./mvnw -q -B -DskipTests package && \
        cp $(ls target/reconx-*.jar | grep -v '\.original$' | head -n 1) app.jar; \
    fi

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S reconx && adduser -S reconx -G reconx
USER reconx
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
