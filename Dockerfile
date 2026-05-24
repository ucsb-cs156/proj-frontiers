FROM maven:3.9.16-eclipse-temurin-21-noble AS builder

WORKDIR /home/app

# Copy of pom & package is a cheap hack to have npm and maven dependencies already loaded;
# Most of the time, they're unlikely to change, so tying them to the file copies allows us not to
# have to reload them constantly.

COPY pom.xml .

RUN mvn --no-transfer-progress dependency:resolve

COPY frontend/package-lock.json frontend/package.json frontend/

RUN mvn --no-transfer-progress -Pproduction com.github.eirslett:frontend-maven-plugin:install-node-and-npm@install-node-and-npm com.github.eirslett:frontend-maven-plugin:npm@npm-install

COPY . .

RUN mvn --no-transfer-progress -B -Pproduction -DskipTests -Dcache.use=true package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add bash curl

COPY --from=builder /home/app/target/frontiers-1.0.0.jar .

COPY --from=builder /home/app/startup.sh .

ENTRYPOINT ["./startup.sh", "frontiers-1.0.0.jar"]