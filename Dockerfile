# Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y --no-install-recommends maven && \
    mvn -q package -DskipTests && \
    rm -rf /var/lib/apt/lists/*

# Run stage — Debian-based JRE (Alpine breaks JSP compilation in fat JARs)
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 10001 app
COPY --from=build /app/target/smart-tour-planner-1.0.0.war app.war
USER app
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m"
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.awt.headless=true -Djava.io.tmpdir=/tmp -jar app.war"]
