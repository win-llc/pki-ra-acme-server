FROM openjdk:14-jdk-alpine
ENV ENV=dev
VOLUME /tmp
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=dev -Djava.security.egd=file:/dev/./urandom","/app.jar"]