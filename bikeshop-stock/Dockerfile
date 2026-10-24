# Imagem do bikeshop-stock (slide 06). O JAR é compilado antes:
#   .\mvnw.cmd package -DskipTests
#   docker build -f bikeshop-stock/Dockerfile -t bikeshop-stock:1.0 .
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="bikeshop-stock" org.opencontainers.image.description="BikeShop: stock e reservas"

# Utilizador sem privilégios (FP03)
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

EXPOSE 9082

ADD bikeshop-stock/target/bikeshop-stock.jar bikeshop-stock.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "bikeshop-stock.jar"]
