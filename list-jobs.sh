#!/bin/sh

toolforge-jobs list --output long 2> /dev/null || crontab -l || cat spacemedia/src/main/resources/crontab.txt

