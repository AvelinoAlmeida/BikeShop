# Imagem do bikeshop-envios (slide 06). O JAR é compilado antes:
#   .\mvnw.cmd package -DskipTests
#   docker build -f bikeshop-envios/Dockerfile -t bikeshop-envios:1.0 .
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="bikeshop-envios" org.opencontainers.image.description="BikeShop: envios das encomendas"

# Utilizador sem privilégios (FP03)
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# Pasta dos ficheiros de log (logback-spring.xml), escrita pelo utilizador sem privilégios
RUN mkdir logs && chown spring:spring logs

EXPOSE 9086

ADD bikeshop-envios/target/bikeshop-envios.jar bikeshop-envios.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "bikeshop-envios.jar"]
