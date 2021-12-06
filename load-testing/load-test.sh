#!/usr/bin/env bash

TIMESTAMP=$(date +%s)

echo "Load testing for 5 minutes..."

mkdir -p "dashboard-$TIMESTAMP"
jmeter -n -t load.jmx -l "loadtest-$TIMESTAMP.csv" -e -o "dashboard-$TIMESTAMP"

echo "Opening results..."
open "dashboard-$TIMESTAMP/index.html"
