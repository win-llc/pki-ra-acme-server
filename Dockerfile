FROM openjdk:14-jdk-alpine
ENV ENV=dev
VOLUME /tmp
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-Dspring.profiles.active=**${ENV}** -Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]