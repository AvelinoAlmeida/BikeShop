# Imagem do bikeshop-gateway (slide 06). O JAR é compilado antes:
#   .\mvnw.cmd package -DskipTests
#   docker build -f bikeshop-gateway/Dockerfile -t bikeshop-gateway:1.0 .
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="bikeshop-gateway" org.opencontainers.image.description="BikeShop: entrada única da API e interface web"

# Utilizador sem privilégios (FP03)
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

EXPOSE 9080

ADD bikeshop-gateway/target/bikeshop-gateway.jar bikeshop-gateway.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "bikeshop-gateway.jar"]
