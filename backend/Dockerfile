FROM eclipse-temurin:23-jre
WORKDIR /app

# OpenTelemetry Java agent (if needed, should be downloaded in GH actions or base image)
# COPY opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

COPY target/*.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]
