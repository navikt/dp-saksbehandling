FROM cgr.dev/chainguard/wolfi-base

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"


RUN apk update && apk add openjdk-21-default-jvm
COPY build/libs/*-all.jar app.jar
#RUN apk update && apk add ls

ENTRYPOINT ["java", "-jar", "/app.jar"]