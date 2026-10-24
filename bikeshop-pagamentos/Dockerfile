# Imagem do bikeshop-pagamentos (slide 06). O JAR é compilado antes:
#   .\mvnw.cmd package -DskipTests
#   docker build -f bikeshop-pagamentos/Dockerfile -t bikeshop-pagamentos:1.0 .
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="bikeshop-pagamentos" org.opencontainers.image.description="BikeShop: pagamentos simulados"

# Utilizador sem privilégios (FP03)
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

EXPOSE 9083

ADD bikeshop-pagamentos/target/bikeshop-pagamentos.jar bikeshop-pagamentos.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "bikeshop-pagamentos.jar"]
