#!/bin/sh

cd /data/project/spacemedia

crontab -r

cd spacemedia && git pull
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./mvnw -ntp -Pweb -Pjobs clean package -Dspring.profiles.active=toolforge -DskipTests
./mvnw -ntp org.apache.maven.plugins:maven-help-plugin:3.4.1:evaluate -Dexpression=project.version -q -DforceStdout > version.txt

crontab src/main/resources/crontab.txt

cd -

