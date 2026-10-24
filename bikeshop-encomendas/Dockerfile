# Imagem do bikeshop-encomendas (slide 06). O JAR é compilado antes:
#   .\mvnw.cmd package -DskipTests
#   docker build -f bikeshop-encomendas/Dockerfile -t bikeshop-encomendas:1.0 .
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="bikeshop-encomendas" org.opencontainers.image.description="BikeShop: encomendas (orquestra a SAGA)"

# Utilizador sem privilégios (FP03)
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# Pasta dos ficheiros de log (logback-spring.xml), escrita pelo utilizador sem privilégios
RUN mkdir logs && chown spring:spring logs

EXPOSE 9084

ADD bikeshop-encomendas/target/bikeshop-encomendas.jar bikeshop-encomendas.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "bikeshop-encomendas.jar"]
