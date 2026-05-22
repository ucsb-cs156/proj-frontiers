FROM maven:3.9.16-eclipse-temurin-21-noble

WORKDIR /home/app

COPY . .

RUN mvn --no-transfer-progress -B -Pproduction -DskipTests clean package

ENTRYPOINT ["./startup.sh","target/frontiers-1.0.0.jar"]

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=0 /home/app/target/frontiers-1.0.0.jar .

COPY --from=0 /home/app/startup.sh .

ENTRYPOINT ["./startup.sh", "frontiers-1.0.0.jar"]