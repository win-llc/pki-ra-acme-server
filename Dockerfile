FROM openjdk:8-jdk-alpine
ADD "/build/libs/ACME Server-1.0-SNAPSHOT.jar" app.jar
VOLUME /tmp
RUN echo "Configured..."
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]