FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system oneqa && useradd --system --gid oneqa oneqa
COPY --from=build /workspace/target/*.jar /app/oneqa.jar
USER oneqa
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/oneqa.jar"]
