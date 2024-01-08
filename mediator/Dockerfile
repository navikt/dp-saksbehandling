FROM cgr.dev/chainguard/jre-lts:latest

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/* /app.jar

#USER nobody
ENTRYPOINT ["java", "-jar", "/app.jar"]