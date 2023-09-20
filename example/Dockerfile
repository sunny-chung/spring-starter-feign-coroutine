FROM amazoncorretto:17

COPY build/libs/example-0.0.1-SNAPSHOT.jar /wd/a.jar
CMD java "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" -jar /wd/a.jar
