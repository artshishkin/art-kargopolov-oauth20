FROM openjdk:11-jre-slim as builder
WORKDIR application
ADD maven/${project.build.finalName}.jar ${project.build.finalName}.jar
RUN java -Djarmode=layertools -jar ${project.build.finalName}.jar extract

FROM openjdk:11-jre-slim

RUN apt update && apt install -y curl && apt install -y jq

HEALTHCHECK  --interval=5s --timeout=2s --start-period=10s --retries=12 \
    CMD curl -f localhost:8080/actuator/health | jq --exit-status -n 'inputs | if has("status") then .status=="UP" else false end' > /dev/null || exit 1

LABEL PROJECT_NAME=${project.artifactId} \
      PROJECT=${project.id}

EXPOSE 8080

ENV SERVER_PORT=8080

WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
RUN true #Fake layer to fix GitHub issue `layer does not exist`
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "org.springframework.boot.loader.JarLauncher"]