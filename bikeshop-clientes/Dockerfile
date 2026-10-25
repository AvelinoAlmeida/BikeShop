# Imagem do bikeshop-clientes (slide 06). O JAR é compilado antes:
#   .\mvnw.cmd package -DskipTests
#   docker build -f bikeshop-clientes/Dockerfile -t bikeshop-clientes:1.0 .
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="bikeshop-clientes" org.opencontainers.image.description="BikeShop: clientes e moradas"

# Utilizador sem privilégios (FP03)
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# Pasta dos ficheiros de log (logback-spring.xml), escrita pelo utilizador sem privilégios
RUN mkdir logs && chown spring:spring logs

EXPOSE 9085

ADD bikeshop-clientes/target/bikeshop-clientes.jar bikeshop-clientes.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "bikeshop-clientes.jar"]
