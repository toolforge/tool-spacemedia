#!/bin/sh
kubectl get pods || ps -afe | grep java | grep -v grep

