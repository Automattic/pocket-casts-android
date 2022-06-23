#!/bin/sh

echo "Running Spotless..."
./gradlew spotlessCheck
RESULT=$?

if [ $RESULT -ne 0 ]; then
  echo ""
  echo "Spotless found format violations, so aborting the commit and applying formatting changes..."
  # using "> /dev/null/" because --quiet doesn't suppress ALL non-error output
  ./gradlew spotlessApply > /dev/null

  echo "Recommended changes from spotless have been applied."
fi

exit $RESULT
