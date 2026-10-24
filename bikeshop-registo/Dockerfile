# Imagem do bikeshop-registo (slide 06). O JAR é compilado antes:
#   .\mvnw.cmd package -DskipTests
#   docker build -f bikeshop-registo/Dockerfile -t bikeshop-registo:1.0 .
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="bikeshop-registo" org.opencontainers.image.description="BikeShop: registo de serviços (Eureka)"

# Utilizador sem privilégios (FP03)
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

EXPOSE 9761

ADD bikeshop-registo/target/bikeshop-registo.jar bikeshop-registo.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "bikeshop-registo.jar"]
