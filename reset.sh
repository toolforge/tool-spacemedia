#!/bin/sh
cd /data/project/spacemedia

toolforge jobs flush 2> /dev/null

rm -f cron-29.* && \
rm -f logs/spacemedia* && \
rm -Rf spacemedia && \
git clone https://gitlab.wikimedia.org/toolforge-repos/spacemedia.git --single-branch --branch=develop-0.5.x && \
./upgrade.sh

cd -

