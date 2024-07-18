#!/bin/bash
command -v kubectl &> /dev/null && kubectl get pods || ps -afe | grep java | grep -v grep

