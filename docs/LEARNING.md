# Learning guide: how Brokemon is put together

You asked for the full app this time, so it's all here. To keep this a learning project, work through the roadmap milestone by milestone: read the files listed, break something on purpose, and fix it. Each section names the concept and what to search for (developer.android.com, Philipp Lackner and Coding in Flow all cover these).

## M0: Foundation
Files: `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`
Study: version catalogs, AGP 9 built-in Kotlin, the KSP plugin, `buildTypes` + R8, the manifest merger (look at how `tools:node="remove"` strips INTERNET).
Search: "Gradle version catalog Android", "AGP built-in Kotlin", "manifest merger tools:node".

## M1: Data model
Files: `data/model/*`, `data/local/Converters.kt`
Study: `@Entity`, why lists need `@TypeConverter`s, why dates are `Long`. Note `Bro.types` and `dexNumber` are computed properties, not columns.
Try: add a `nickname` field. Room will make you bump the DB version, so look up "Room auto migration".

## M2: Catch flow
Files: `ui/catchbro/BroViewModel.kt`, `CatchBroScreen.kt`, `CatchOverlay.kt`
Study: a single `MutableStateFlow<BroState>` updated with `_state.update { it.copy(...) }`, plus unidirectional data flow (the screen only calls VM functions). Your planned functions `onMoveToggled`, `onCustomMoveInputChanged` and `addCustomMove` are in there; compare them with what you would have written.
Animation: `Animatable` sequenced inside one `LaunchedEffect` (drop, then shake x3, then burst).

## M3: Card display
Files: `ui/components/BroCard.kt`, `BroSprite.kt`, `AvatarBuilder.kt`, `Effects.kt`, `DexChrome.kt`, `domain/HumanSprite.kt`, `data/model/BroLook.kt`
Study: `rememberInfiniteTransition`, `Modifier.drawBehind` / `drawWithContent`, `composed {}` modifiers, `FilterQuality.None` for crisp pixels.
Try: add a new hairstyle. Append it to `LookOptions.hairStyles` (append only, because indices are what gets stored) and draw it in `HumanSprite.drawFrontHair`.
Also study: `app/schemas/` and the `AutoMigration(1 → 2)` in `BroDatabase`. That's how the `look` column was added without wiping anyone's data.

## M4: Memory log
Files: `data/MediaStorage.kt`, `ui/detail/BroDetailViewModel.kt` (prepareCapture/onCaptureResult), `res/xml/file_paths.xml`
Study: `FileProvider`, `ActivityResultContracts.TakePicture` / `CaptureVideo` / `PickVisualMedia`, and why the pending file path goes into `SavedStateHandle` (the camera app can get your process killed).

## M5: Evolution
Files: `domain/Evolution.kt`, `ui/detail/EvolutionOverlay.kt`, `UserPrefs.seenStage`
Study: the stage is derived, never stored. Only "which animation has the user already seen" is stored, and that lives in DataStore.

## M6: Squads
Files: `domain/TypeMatchups.kt`, `ui/squads/*`
Study: `combine()` of several Flows into one UI state.

## M7: QR trading
Files: `share/QrCodec.kt`, `share/QrBitmap.kt`, `ui/trade/*`
Study: why decode validates and clamps everything (a QR is untrusted input), and the Google code scanner vs. a CameraX scanner.

## M8: Engagement
Files: `domain/CheckOnBro.kt`, `domain/Wrapped.kt`, `ui/engage/*`
Study: weighted random selection, `HorizontalPager`, and reading pager offsets inside `graphicsLayer {}` so swiping doesn't recompose.

## Retention features (backup, sharing, widget, reminders)
- Backup: `data/backup/BackupManager.kt`, `res/xml/data_extraction_rules.xml`, `backup_rules.xml`. Study: Storage Access Framework (`CreateDocument` / `OpenDocument`), `ZipOutputStream`, "zip slip", Android Auto Backup rules, `withTransaction`.
- Share images: `ui/share/StoryImages.kt`. Study: `android.graphics.Canvas`, `Paint`, `Shader`s, `FileProvider` + `Intent.ACTION_SEND`.
- Widget: `widget/BroOfTheDayWidget.kt`. Study: Jetpack Glance (`GlanceAppWidget`, `ActionCallback`), app-widget provider XML.
- Reminders: `notify/*`, `domain/Reminders.kt`. Study: `CoroutineWorker` + periodic work, notification channels, the POST_NOTIFICATIONS runtime permission, `PendingIntent` flags, `BroadcastReceiver.goAsync()`.

## Holo, voice, events
- Holo tilt: `ui/components/Holo.kt`. Study: `SensorManager` + `TYPE_GAME_ROTATION_VECTOR`, `getRotationMatrixFromVector` / `getOrientation`, low-pass filtering, `graphicsLayer { rotationX/Y }`, `BlendMode.Screen/Overlay`. Note: tilt is read in the draw phase so it never recomposes.
- Voice: `audio/VoiceRecorder.kt`, `ui/components/Audio.kt`. Study: `MediaRecorder`, `MediaPlayer`, the RECORD_AUDIO runtime permission, `DisposableEffect` for releasing players.
- Events: `domain/SeasonEvents.kt`. Study: `java.time.chrono.HijrahDate` (the Umm al-Qura calendar), then the pure-function + unit-test pattern in `SeasonEventsTest`.
- Schema: `docs/SCHEMA.md` and `AutoMigration(2 → 3)`.

## Rooms, card handling, filters
- Room: `domain/RoomRenderer.kt` (pixel scene), `HumanSprite.renderFullBody`, `ui/room/*`. Study: `SharedTransitionLayout` + `Modifier.sharedBounds` (card → room morph), `AnimatedVisibilityScope.animateEnterExit`, `combinedClickable` (long-press).
- Card handling: `ui/components/Holo.kt` `dragToTilt`. Study: `pointerInput` + `awaitEachGesture`, `Animatable`/`animate` with `spring`.
- Filters: `ui/home/HomeViewModel.kt` (`BroFilter`, `SortOrder`) + `FilterSheet.kt`. Study: `ModalBottomSheet`, keeping filter state in the ViewModel.

## Trainer, Journal, views, wild bros
- Intro: `ui/onboarding/OnboardingScreen.kt`. Study: a typewriter effect with `LaunchedEffect` + `delay`, `AnimatedContent` between steps, `BackHandler`.
- Trainer Card + Journal: `data/model/Trainer.kt`, `domain/Journal.kt`, `ui/trainer/*`. Study: storing a small object as JSON in DataStore, deriving progress from data instead of storing it (only claims are saved), `combine` of four flows.
- Coach marks: `ui/components/CoachMark.kt`. Study: `staticCompositionLocalOf` to hand a dependency (the hint store) down without threading it through every screen.
- Dex views: `ui/home/HomeScreen.kt`. Study: `AnimatedContent` for the view switch, `HorizontalPager` + `graphicsLayer` driven by `currentPageOffsetFraction` for the binder.
- Stat hexagon: `ui/components/StatHexagon.kt`. Study: `Canvas`, `Path`, polar coordinates, `rememberTextMeasurer` + `drawText`.
- Wild bro: `ui/wild/*`. Study: `TYPE_ACCELEROMETER` + `LifecycleResumeEffect` (listen only while visible), an explicit-package `Intent` for WhatsApp with a share-sheet fallback, and a single 0..1 `Animatable` driving a whole timeline.

## M9–M10: Privacy and publishing
See `docs/PLAY_STORE.md` and `docs/privacy-policy.md`.

## Tests
`app/src/test/...` holds JVM unit tests for all the pure logic. Run `./gradlew testDebugUnitTest`. Good next exercise: write a test for `TypeMatchups.clash`.
