#!/usr/bin/env bash
# Compile and run this phase (needs JDK 17+).
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out jobscheduler.Main
