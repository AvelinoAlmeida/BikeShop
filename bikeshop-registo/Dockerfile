# Imagem do bikeshop-registo (slide 06). O JAR é compilado antes:
#   .\mvnw.cmd package -DskipTests
#   docker build -f bikeshop-registo/Dockerfile -t bikeshop-registo:1.0 .
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="bikeshop-registo" org.opencontainers.image.description="BikeShop: registo de serviços (Eureka)"

# Utilizador sem privilégios (FP03)
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# Pasta dos ficheiros de log (logback-spring.xml), escrita pelo utilizador sem privilégios
RUN mkdir logs && chown spring:spring logs

EXPOSE 9761

ADD bikeshop-registo/target/bikeshop-registo.jar bikeshop-registo.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "bikeshop-registo.jar"]
