FROM openjdk:8u212-jre-alpine
VOLUME /tmp
#ARG DEPENDENCY=target/dependency
#COPY ${DEPENDENCY}/BOOT-INF/lib /pogorobot/lib
#COPY ${DEPENDENCY}/META-INF /pogorobot/META-INF
#COPY ${DEPENDENCY}/BOOT-INF/classes /pogorobot
#ENTRYPOINT ["java","-cp","pogorobot:pogorobot/lib/*","pogorobot.PoGoRobotApplication"]
# timezone env with default
ENV TZ Europe/Berlin
ARG JAR_FILE
RUN apk -U --no-cache upgrade
COPY config /config
COPY target/${JAR_FILE} pogorobot.jar
#ENTRYPOINT ["java","-jar","/pogorobot.jar"]
ENTRYPOINT ["java", \
"-Dcom.sun.management.jmxremote", \
"-Dcom.sun.management.jmxremote.port=8010", \
"-Dcom.sun.management.jmxremote.local.only=false", \
"-Dcom.sun.management.jmxremote.authenticate=false", \
"-Dcom.sun.management.jmxremote.ssl=false", \
"-Dcom.sun.management.jmxremote.rmi.port=8010", \
"-Djava.rmi.server.hostname=127.0.0.1", \
"-Dext.properties.dir=file:/config", \
"-Xmx700m", \
"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000", \
"-jar", \
"/pogorobot.jar"]
EXPOSE 8010 8080 8000