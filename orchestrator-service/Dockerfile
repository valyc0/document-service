FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
VOLUME /data

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
