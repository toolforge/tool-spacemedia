#!/bin/sh
tail -f logs/${1:-spacemedia}.log| sed \
    -e 's/\(.*DEBUG.*\)/\x1B[37m\1\x1B[39m/' \
    -e 's/\(.*INFO.*\)/\x1B[36m\1\x1B[39m/' \
    -e 's/\(.*WARN.*\)/\x1B[33m\1\x1B[39m/' \
    -e 's/\(.*ERROR.*\)/\x1B[31m\1\x1B[39m/'

