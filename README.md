# House Hunter

An Android app for cataloging the flats you visit while house hunting. Each flat gets its own entry with a GPS location. Photos, videos and text/audio/video notes are captured or imported straight into that entry, and each item is geotagged. Agent-shared media (e.g. from WhatsApp) can be shared into an entry.

The core model (Collection → Entry → Attachments/Notes, with template-defined fields) is generic, so other templates such as an Ideas/Journal can reuse it later.

See [docs/PLAN.md](docs/PLAN.md) for the roadmap and milestone progress.

## Tech
Kotlin · Jetpack Compose (Material 3) · Hilt · Room · Navigation Compose · CameraX · Google Maps/Places · WorkManager + Google Drive backup

## Setup
1. Install Android Studio (with JDK 17+) and the Android SDK (platform 36).
2. Clone the repo and open it in Android Studio. It creates `local.properties` with your `sdk.dir`.
3. *(Needed from M7 on)* Create `secrets.properties` in the project root:
   ```properties
   MAPS_API_KEY=your-google-cloud-api-key
   ```
   This file is git-ignored. Never commit real keys; `secrets.defaults.properties` holds placeholders.

## Build & test
```bash
./gradlew assembleDebug   # build the debug APK
./gradlew test            # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device/emulator)
```
