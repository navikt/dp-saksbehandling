FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:a282d0fe90a0d0755a130930277123ba6314c3b5fa71c1cfbf83fd6ac4fd2247

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/mediator-all.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]