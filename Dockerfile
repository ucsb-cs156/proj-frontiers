FROM maven:3.9.16-eclipse-temurin-25-noble AS builder

WORKDIR /home/app

# Copy of pom & package is a cheap hack to have npm and maven dependencies already loaded;
# Most of the time, they're unlikely to change, so tying them to the file copies allows us not to
# have to reload them constantly.

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2/repository mvn dependency:go-offline -B

COPY frontend/package-lock.json frontend/package.json frontend/

RUN --mount=type=cache,target=/root/.m2/repository --mount=type=cache,target=/root/.npm mvn -Pproduction com.github.eirslett:frontend-maven-plugin:install-node-and-npm@install-node-and-npm com.github.eirslett:frontend-maven-plugin:npm@npm-install -B

COPY . .

RUN --mount=type=cache,target=/root/.m2/repository mvn -Pproduction -DskipTests -Dcache.use=true -Dgcf.skip=true package -B

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN apk add bash curl

COPY --from=builder /home/app/startup.sh .

COPY --from=builder /home/app/target/frontiers-1.0.0.jar .

ENTRYPOINT ["./startup.sh", "frontiers-1.0.0.jar"]