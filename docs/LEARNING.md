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
Files: `ui/components/BroCard.kt`, `BroSprite.kt`, `Effects.kt`, `DexChrome.kt`, `domain/SpriteGenerator.kt`
Study: `rememberInfiniteTransition`, `Modifier.drawBehind` / `drawWithContent`, `composed {}` modifiers, `FilterQuality.None` for crisp pixels.
Try: change a mask in `SpriteGenerator` and watch every bro's silhouette change.

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

## M9–M10: Privacy and publishing
See `docs/PLAY_STORE.md` and `docs/privacy-policy.md`.

## Tests
`app/src/test/...` holds JVM unit tests for all the pure logic. Run `./gradlew testDebugUnitTest`. Good next exercise: write a test for `TypeMatchups.clash`.
