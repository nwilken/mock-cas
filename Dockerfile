ARG APP_VERSION=0.0.1-SNAPSHOT

FROM maven:3-amazoncorretto as build

WORKDIR /usr/src/app
COPY src ./src
COPY pom.xml .
RUN mvn -ntp clean package

FROM amazoncorretto:11 as final
LABEL maintainer="Nate Wilken <wilken@asu.edu>"

ARG APP_VERSION

WORKDIR /app
COPY mock-cas.yml /app/
COPY --from=build /usr/src/app/target/mock-cas-${APP_VERSION}.jar /app/mock-cas.jar

CMD ["java", "-jar", "mock-cas.jar", "server", "mock-cas.yml"]