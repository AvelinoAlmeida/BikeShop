# Imagem do bikeshop-catalogo (slide 06). O JAR é compilado antes:
#   .\mvnw.cmd package -DskipTests
#   docker build -f bikeshop-catalogo/Dockerfile -t bikeshop-catalogo:1.0 .
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="bikeshop-catalogo" org.opencontainers.image.description="BikeShop: catálogo de peças para bicicletas elétricas"

# Utilizador sem privilégios (FP03)
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# Pasta dos ficheiros de log (logback-spring.xml), escrita pelo utilizador sem privilégios
RUN mkdir logs && chown spring:spring logs

EXPOSE 9081

ADD bikeshop-catalogo/target/bikeshop-catalogo.jar bikeshop-catalogo.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "bikeshop-catalogo.jar"]
