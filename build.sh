#!/bin/sh

cd /data/project/spacemedia

[ -d spacemedia ] || git clone --single-branch --branch develop-0.5.x https://gitlab.wikimedia.org/toolforge-repos/spacemedia.git

crontab -r

cd spacemedia && git pull
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./mvnw -ntp -Pweb -Pjobs clean package -Dspring.profiles.active=toolforge -DskipTests
./mvnw -ntp org.apache.maven.plugins:maven-help-plugin:3.4.1:evaluate -Dexpression=project.version -q -DforceStdout > version.txt

crontab src/main/resources/crontab.txt

cd -

