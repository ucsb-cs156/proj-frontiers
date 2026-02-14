FROM dejensen747/cs156-base:1.0

COPY . /home/app

RUN mvn --no-transfer-progress -B -Pproduction -DskipTests -f /home/app/pom.xml clean package

ENTRYPOINT ["/home/app/startup.sh","/home/app/target/frontiers-1.0.0.jar"]
