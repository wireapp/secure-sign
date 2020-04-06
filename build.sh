#!/usr/bin/env bash
#docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
mvn package -DskipTests=true -Dmaven.javadoc.skip=true
docker build -t $DOCKER_USERNAME/swisscom:latest .
docker push $DOCKER_USERNAME/swisscom
kubectl delete pod -l name=swisscom -n prod
kubectl get pods -l name=swisscom -n prod
