FROM eclipse-temurin:21-jdk-alpine-3.22 AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine-3.22 AS runtime
RUN addgroup -S glazr && adduser -S -G glazr -u 10001 glazr
WORKDIR /app
COPY --from=build --chown=glazr:glazr /workspace/target/glazr-backend-0.1.0.jar app.jar
USER 10001:10001
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
