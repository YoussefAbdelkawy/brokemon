# Google Play release checklist

## Already handled in the code
- [x] targetSdk 36 (meets the API 36 target requirement for new apps from Aug 31, 2026); compileSdk 37.
- [x] Android App Bundle via `./gradlew bundleRelease`, with R8 minification and resource shrinking.
- [x] Release signing from git-ignored `keystore.properties`. Enroll in **Play App Signing** when you upload.
- [x] Adaptive launcher icon with a monochrome layer (themed icons) and a SplashScreen API splash.
- [x] Edge-to-edge layout (mandatory when targeting 35+) and predictive back (`enableOnBackInvokedCallback`).
- [x] **No dangerous permissions.** The merged manifest declares no INTERNET, CAMERA or READ_MEDIA_*. Camera capture uses intents, gallery access uses the system photo picker, and QR scanning runs inside Play services. This keeps Brokemon clear of the Photo & Video Permissions policy.
- [x] `android.hardware.camera` declared `required=false`, so devices without a camera can still install the app.
- [x] Backups disabled (`allowBackup=false` + data extraction rules that exclude everything).
- [x] In-app privacy policy, delete-all-data, and an onboarding consent about photos of friends.
- [x] Original art only. The font is OFL-licensed and credited in-app (Settings → Licenses). No Nintendo/Pokémon names or trade dress; the catch device is a cube, not a ball.
- [x] Store copy and in-app text avoid "Pokémon"/"Pokédex"; the app says "Brodex".

## What you still need to do in Play Console
1. **Create the upload keystore** (Android Studio → Build → Generate Signed Bundle) and fill in `keystore.properties`. Back up the .jks file.
2. **Host the privacy policy.** Replace `CONTACT_EMAIL_HERE` in `docs/privacy-policy.md`, publish it (for example with GitHub Pages) and paste the URL into the Console.
3. **Data safety form** (suggested answers, verify before submitting):
   - Does your app collect or share user data? → **No.** Data the app stores only on-device, which never leaves the device unless the user shows a QR code, does not count as "collected". User-initiated QR display counts as user-initiated sharing, not developer collection.
   - Is data encrypted in transit? → Not applicable (no network).
   - Can users request deletion? → Yes, in-app (Settings → Delete all data).
   - Note: the Play services code scanner is provided by Google. Check Google's current ML Kit / code scanner Data safety guidance when you submit, and declare anything it says SDK users must disclose.
4. **Content rating** questionnaire: no violence, no user-to-user chat, no location sharing. User-generated photos are stored locally only.
5. **Target audience**: 13+ (or 18+), not designed for children. This avoids Families policy requirements.
6. **Ads**: declare "No ads".
7. **Store listing**: original screenshots (`docs/screenshots` is a starting point; capture real ones on a device), a 512×512 icon and a 1024×500 feature graphic. Don't put "Pokémon" in the title, description or tags.
8. **Testing track**: new personal developer accounts must run a closed test with at least 12 testers for 14 days before production access. Start with internal testing to sanity-check, then closed testing.
9. **Pre-launch report**: review the automated device tests and accessibility suggestions after the first upload.

## Open product decisions that affect the listing
- Private friend-group app vs. public release changes how strongly you word the photo-consent language.
- Evolution thresholds (currently 0 / 20 / 40) should be tuned with real usage before launch.
