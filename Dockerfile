FROM maven:3.9.16-eclipse-temurin-21-noble AS builder

WORKDIR /home/app

COPY pom.xml .

RUN ["mvn", "dependency:resolve"]

COPY . .

RUN mvn --no-transfer-progress -B -Pproduction -DskipTests clean package

ENTRYPOINT ["./startup.sh","target/frontiers-1.0.0.jar"]

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add bash

COPY --from=builder /home/app/target/frontiers-1.0.0.jar .

COPY --from=builder /home/app/startup.sh .

ENTRYPOINT ["./startup.sh", "frontiers-1.0.0.jar"]