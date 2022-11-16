FROM openjdk:17-oracle
ENV ENV=dev
VOLUME /tmp
COPY build/libs/pki-ra-acme-server-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=dev","-Djava.security.egd=file:/dev/./urandom","-Djavax.net.ssl.trustStoreType=JKS", "-Djavax.net.ssl.trustStore=/ssl/trust.jks","/app.jar"]