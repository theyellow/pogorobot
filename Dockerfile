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
ENTRYPOINT ["java","-Dext.properties.dir=file:/config","-Xmx700m","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000","-jar","/pogorobot.jar"]