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

# Exit early if gitleaks is not installed
if ! [ -x "$(command -v gitleaks)" ]; then

  # Check if user has access to secrets
  if [ -e "$HOME/.mobile-secrets/" ]; then
    RED='\033[0;31m'
    NO_COLOR='\033[0m'
    printf "\n${RED}ERROR: You have access to PocketCasts secrets, so you must install gitleaks.${NO_COLOR}\n"
    exit 1
  else
    echo "Gitleaks not installed. Skipping gitleaks check..."
    exit 0
  fi
fi

gitleaks protect -v --staged
RESULT=$?

if [ $RESULT -ne 0 ]; then
  echo ""
  echo "Warning: gitleaks has detected sensitive information in your changes. Aborting commit."
  exit $RESULT
fi

exit $RESULT
