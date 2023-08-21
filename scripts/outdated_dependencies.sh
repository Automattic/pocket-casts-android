#!/usr/bin/env bash
set -e # fail on error

# run the script in the parent directory
BASEDIR=$(dirname "$0")
cd "$BASEDIR"/..

set -x # output commands when running them

cd ..
./gradlew dependencyUpdates -Drevision=release