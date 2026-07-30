FROM openapitools/openapi-generator-cli:latest AS openapi-generator
WORKDIR /app
COPY . .
RUN openapi-generator-cli generate \
      -i trading-service/openapi.yaml \
      -g scala-http4s-server \
      -o trading-service/generated/server \
      --additional-properties=packageName=trading_service.generated.server,packageVersion=1.0.0,excludeSbt=true \
    && rm -f trading-service/generated/server/build.sbt
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

RUN apt-get update && apt-get install -y curl gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" \
      | gpg --dearmor | tee /etc/apt/trusted.gpg.d/scalasbt-release.gpg > /dev/null && \
    apt-get update && apt-get install -y sbt

COPY . .
COPY --from=openapi-generator /app/trading-service/generated/server trading-service/generated/server
RUN sbt tradingService/assembly

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/trading-service/target/scala-3.8.4/trading-service-assembly-*.jar app.jar
EXPOSE 3001
CMD ["java", "-jar", "app.jar"]