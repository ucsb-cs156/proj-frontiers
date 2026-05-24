FROM maven:3.9.16-eclipse-temurin-21-noble AS builder

WORKDIR /home/app

# Copy of pom & package is a cheap hack to have npm and maven dependencies already downloaded; no need
# to redownload every time and even though CI reinstalls from scratch it at least won't have to redownload them

COPY pom.xml .

RUN ["mvn", "dependency:resolve"]

COPY frontend/package-lock.json frontend/package.json frontend/

RUN mvn com.github.eirslett:frontend-maven-plugin:install-node-and-npm com.github.eirslett:frontend-maven-plugin:npm -Dfrontend.npm.arguments=ci -DworkingDirectory=frontend

COPY . .

RUN mvn --no-transfer-progress -B -Pproduction -DskipTests clean package

ENTRYPOINT ["./startup.sh","target/frontiers-1.0.0.jar"]

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add bash curl

COPY --from=builder /home/app/target/frontiers-1.0.0.jar .

COPY --from=builder /home/app/startup.sh .

ENTRYPOINT ["./startup.sh", "frontiers-1.0.0.jar"]