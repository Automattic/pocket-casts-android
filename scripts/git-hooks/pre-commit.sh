#!/bin/sh


##################
# Spotless
##################

echo "Running Spotless..."
./gradlew spotlessCheck
RESULT=$?

if [ $RESULT -ne 0 ]; then
  echo ""
  echo "Spotless found format violations, so aborting the commit and applying formatting changes..."
  # using "> /dev/null/" because --quiet doesn't suppress ALL non-error output
  ./gradlew spotlessApply > /dev/null

  echo "Recommended changes from spotless have been applied."
  exit $RESULT
fi

##################
# gitleaks
##################

# Use sentry.properties file as a proxy for determining if the contributor has
# access to secrets and should run gitleaks before committing
if [ ! -f "sentry.properties" ]; then
  echo "Skipping gitleaks check..."
  exit $RESULT
fi

if ! command -v gitleaks &> /dev/null
then
  echo ""
  echo "Error: gitleaks script not found, please install it (https://github.com/zricethezav/gitleaks)"
  exit 1
fi

gitleaks protect -v --staged
RESULT=$?

if [ $RESULT -ne 0 ]; then
  echo ""
  echo "Warning: gitleaks has detected sensitive information in your changes. Aborting commit."
  exit 1
fi

exit $RESULT
