# Onboarding E2E Tests

This document describes how to run onboarding end-to-end Android instrumentation tests locally and in GitHub Actions.

## Tests Included

- `au.com.shiftyjelly.pocketcasts.account.onboarding.e2e.LogInFullAppTest`
- `au.com.shiftyjelly.pocketcasts.account.onboarding.e2e.OnboardingFullAppTest`

## Local Run

1. Copy credentials template:

   ```bash
   cp onboarding-test.local.properties.example onboarding-test.local.properties
   ```

2. Set values in `onboarding-test.local.properties`:

   ```properties
   onboardingTestEmail=your-test-email@example.com
   onboardingTestPassword=your-test-password
   ```

3. Run the tests:

   ```bash
   ./gradlew :app:connectedDebugAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=au.com.shiftyjelly.pocketcasts.account.onboarding.e2e.LogInFullAppTest,au.com.shiftyjelly.pocketcasts.account.onboarding.e2e.OnboardingFullAppTest
   ```

## GitHub Actions Run

Workflow file: `.github/workflows/android-onboarding-e2e.yml`.

Required GitHub repository secrets:

- `ONBOARDING_TEST_EMAIL`
- `ONBOARDING_TEST_PASSWORD`

The workflow:

- starts an Android emulator (`api-level: 34`)
- runs the two onboarding E2E test classes
- publishes a JUnit summary in the GitHub Actions UI
- uploads Android test reports as workflow artifacts

## Troubleshooting

- `Missing instrumentation argument 'onboardingEmail'` or `'onboardingPassword'`:
  set local properties or CI secrets.
- Flaky UI steps:
  re-run job and inspect artifact reports under `app/build/reports/androidTests/connected/`.
