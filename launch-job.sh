#!/bin/sh
toolforge-jobs run manual-$2 --image tf-jdk17 --no-filelog --cpu 1 --mem 4Gi --command "./run.sh $1 $2"
tail -f logs/$2.log

