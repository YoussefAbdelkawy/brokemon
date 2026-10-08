# Releasing

1. Update `versionCode` (+1) and `versionName` in `app/build.gradle.kts`.
2. Add the release to `CHANGELOG.md` and to `ReleaseNotes.kt` (that list feeds the in-app "What's new" popup).
3. If the database changed: bump `version` in `BroDatabase`, add an `AutoMigration`, commit the new `app/schemas/.../N.json`, and extend `MigrationTest`. Never use `fallbackToDestructiveMigration`.
4. `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`
5. Commit, then tag and push the tag:
   ```
   git tag -a v1.3.0 -m "Brokemon 1.3.0"
   git push origin v1.3.0
   ```
6. Set `feedback_email` in `app/src/main/res/values/strings.xml` before a public release (empty means the email app opens with no recipient).
