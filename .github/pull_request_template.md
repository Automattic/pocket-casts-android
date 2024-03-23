## Description
Adds into the Sleep Timer the feature of sleeping by episodes
Fixes #940

## Testing Instructions
1. Begin any podcast episode
2. Open the miniplayer to expand it
3. Select the Sleep Timer icon in the icon list

## Screenshots or Screencast 
<!-- if applicable -->

## Checklist
- [ ] If this is a user-facing change, I have added an entry in CHANGELOG.md
- [ ] Ensure the linter passes (`./gradlew spotlessApply` to automatically apply formatting/linting)
- [ ] I have considered whether it makes sense to add tests for my changes
- [ ] All strings that need to be localized are in `modules/services/localization/src/main/res/values/strings.xml`
- [ ] Any jetpack compose components I added or changed are covered by compose previews
 
#### I have tested any UI changes...
<!-- If this PR does not contain UI changes, ignore these items -->
- [ ] with different themes
- [ ] with a landscape orientation
- [ ] with the device set to have a large display and font size
- [ ] for accessibility with TalkBack
