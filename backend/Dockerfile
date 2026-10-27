# syntax=docker/dockerfile:1

# Os testes rodam fora da imagem (mvn test), pois o teste de integração usa Docker.
FROM maven:3.9-eclipse-temurin-25 AS construcao
WORKDIR /fonte
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=construcao /fonte/target/autenticacao.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
