# Imagem do bikeshop-encomendas (slide 06). O JAR é compilado antes:
#   .\mvnw.cmd package -DskipTests
#   docker build -f bikeshop-encomendas/Dockerfile -t bikeshop-encomendas:1.0 .
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="bikeshop-encomendas" org.opencontainers.image.description="BikeShop: encomendas (orquestra a SAGA)"

# Utilizador sem privilégios (FP03)
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

EXPOSE 9084

ADD bikeshop-encomendas/target/bikeshop-encomendas.jar bikeshop-encomendas.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "bikeshop-encomendas.jar"]
