FROM openjdk:8u212-jre-alpine
VOLUME /tmp
# timezone env with default
ENV TZ Europe/Berlin
ARG JAR_FILE
RUN apk -U --no-cache upgrade
#COPY config /config
COPY target/${JAR_FILE} pogorobot.jar
ENTRYPOINT ["java", \
"-Dcom.sun.management.jmxremote", \
"-Dcom.sun.management.jmxremote.port=8810", \
"-Dcom.sun.management.jmxremote.local.only=false", \
"-Dcom.sun.management.jmxremote.authenticate=false", \
"-Dcom.sun.management.jmxremote.ssl=false", \
"-Dcom.sun.management.jmxremote.rmi.port=8810", \
"-Djava.rmi.server.hostname=127.0.0.1", \
"-Dext.properties.dir=file:/config", \
"-Xmx700m", \
"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8800", \
"-jar", \
"/pogorobot.jar"]

# expose webhook (8080) and debug-ports (88*0)
EXPOSE 8810 8080 8800