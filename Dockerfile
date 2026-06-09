# Build stage
FROM eclipse-temurin:19-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && \
    mvn -q package -DskipTests

# Run stage
FROM eclipse-temurin:19-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/smart-tour-planner-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.awt.headless=true -jar app.jar"]
