FROM eclipse-temurin:23-jre
WORKDIR /app

ARG OTEL_JAVA_AGENT_VERSION=2.27.0
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

COPY target/*-with-dependencies.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]
