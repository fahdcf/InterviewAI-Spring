FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /source
COPY pom.xml .
COPY backend/pom.xml backend/pom.xml
COPY desktop/pom.xml desktop/pom.xml
COPY backend/src backend/src
RUN mvn -B -ntp -pl backend -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system interviewai && useradd --system --gid interviewai interviewai
COPY --from=build /source/backend/target/interviewai-backend-1.0.0.jar app.jar
USER interviewai
ENV SERVER_ADDRESS=0.0.0.0
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
