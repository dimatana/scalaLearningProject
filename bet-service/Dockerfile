FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

RUN apt-get update && apt-get install -y curl gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" \
      | gpg --dearmor | tee /etc/apt/trusted.gpg.d/scalasbt-release.gpg > /dev/null && \
    apt-get update && apt-get install -y sbt

COPY . .
RUN sbt betService/assembly

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/bet-service/target/scala-3.8.4/bet-service-assembly-*.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]