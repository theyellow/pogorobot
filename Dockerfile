FROM openjdk:8-jdk-alpine
VOLUME /tmp
#ARG DEPENDENCY=target/dependency
#COPY ${DEPENDENCY}/BOOT-INF/lib /pogorobot/lib
#COPY ${DEPENDENCY}/META-INF /pogorobot/META-INF
#COPY ${DEPENDENCY}/BOOT-INF/classes /pogorobot
#ENTRYPOINT ["java","-cp","pogorobot:pogorobot/lib/*","pogorobot.PoGoRobotApplication"]
ARG JAR_FILE
COPY config /config
COPY target/${JAR_FILE} pogorobot.jar
#ENTRYPOINT ["java","-jar","/pogorobot.jar"]
ENTRYPOINT ["java","-Dext.properties.dir=file:/config","-Xmx700m","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8000","-jar","/pogorobot.jar"]