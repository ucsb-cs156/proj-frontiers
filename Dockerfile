FROM maven:3.9.16-eclipse-temurin-21-noble

WORKDIR /home/app

COPY . .

RUN mvn --no-transfer-progress -B -Pproduction -DskipTests clean package

ENTRYPOINT ["./startup.sh","target/frontiers-1.0.0.jar"]
