#!/bin/sh
watch -n 1 "tail logs/$1.log ; echo ; df -h / ; echo ; ls -l -h /tmp/sm*"

