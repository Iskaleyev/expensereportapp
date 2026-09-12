# Receipt Logger (prototype)

A small Android app: snap a photo of a receipt, type in a sum and a note, and it's saved
to a scrollable history list. Built with Kotlin + Jetpack Compose, Room (local database),
and Coil (image loading). Minimum Android version: 8.0 (API 26).

## What it does

- **Receipts** screen: a list of everything you've logged (thumbnail, amount, note, date/time),
  with a `+` button.
- **New receipt** screen: opens your phone's camera app to take a photo, then two fields
  (Sum, Note) and a Save button. Save is disabled until you've taken a photo and entered
  a sum greater than 0.
- Everything is stored locally on the device (Room database + photos in the app's private
  storage) — there's no backend, login, or sync in this prototype.

## Why there's no APK attached

This was built in a sandboxed cloud environment whose network access is locked down to a
small allowlist of hosts — it cannot reach Google's Maven repository, Maven Central, or
the Android SDK download servers at all (every request to dl.google.com, maven.google.com,
repo1.maven.org, etc. comes back `403 Forbidden`). Building an Android app requires
downloading the Android SDK, the Android Gradle Plugin, and all the AndroidX/Compose/Room
libraries from exactly those hosts, so a real build couldn't be compiled in this sandbox.
Your own machine (or a CI runner) won't have that restriction, so building from here should
just work.

## How to build and run it

The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) is included, so you don't
need Gradle installed separately — it downloads the right version itself on first run.

**Easiest: Android Studio**

1. Install [Android Studio](https://developer.android.com/studio) if you don't have it.
2. Open Android Studio → `Open` → select this `ReceiptLogger` folder.
3. If prompted about the Gradle JDK, use the embedded JDK Android Studio offers
   (`.../Android Studio/jbr`) — no need to install a separate JDK.
4. Let it sync (first sync downloads Gradle + all dependencies — needs internet, takes a
   few minutes).
5. Plug in an Android phone (with USB debugging on) or start an emulator, then click ▶ Run.

**Command line, if you'd rather not use Android Studio:**

```bash
cd ReceiptLogger
./gradlew assembleDebug
# on Windows: gradlew.bat assembleDebug
# APK will be at app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```

You'll still need the Android SDK installed somewhere and a `local.properties` file in this
folder pointing at it (`sdk.dir=/path/to/Android/Sdk`) — Android Studio creates this file
automatically the first time you open the project there, which is why that route is easier
if this is your first Android project.

## Project layout

```
app/src/main/java/com/timur/receiptlogger/
  MainActivity.kt          entry point, sets up the Compose theme
  ReceiptApp.kt             navigation between the two screens
  ReceiptViewModel.kt       holds app state, talks to the database
  data/                     Room entity, DAO, database
  ui/                       the two screens (list, add)
  ui/theme/                 Compose Material 3 theme
  util/ImageUtils.kt        creates the file the camera photo is saved into
```

## Known limitations (it's a prototype)

- No way to edit or delete a saved entry yet.
- No search/filter or totals.
- No CAMERA runtime permission request — on most devices the system camera app handles
  its own permission, but if a photo capture silently fails on your device, that's the
  first thing to add.
- No app icon design — just a placeholder receipt-shaped vector icon.

Happy to add any of the above, wire up cloud sync, or restyle it — just ask.
