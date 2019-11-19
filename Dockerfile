FROM dejankovacevic/bots.runtime:2.10.3

COPY target/swisscom.jar   /opt/swisscom/swisscom.jar
COPY swisscom.yaml         /etc/swisscom/swisscom.yaml
COPY certs/swisscom.jks    /opt/swisscom/swisscom.jks

RUN mkdir /opt/swisscom/signatures

WORKDIR /opt/swisscom

EXPOSE  8080 8081 8082
