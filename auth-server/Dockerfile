FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY auth-server/pom.xml auth-server/pom.xml
RUN mvn -q -pl auth-server -am dependency:go-offline
COPY auth-server/src auth-server/src
RUN mvn -q -pl auth-server -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/auth-server/target/auth-server-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java","-jar","/app/app.jar"]
