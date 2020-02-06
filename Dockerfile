FROM dejankovacevic/bots.runtime:2.10.3

COPY target/swisscom.jar   /opt/swisscom/
COPY swisscom.yaml         /etc/swisscom/
COPY swisscom.jks          /opt/swisscom/

RUN mkdir /opt/swisscom/signatures

WORKDIR /opt/swisscom

EXPOSE  8080 8081 8082
