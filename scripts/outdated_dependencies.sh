#!/usr/bin/env bash
set -e # fail on error
set -x # output commands when running them

cd ..
./gradlew dependencyUpdates -Drevision=release