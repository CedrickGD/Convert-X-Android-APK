# Convert-X (Android)

Fast, offline file converter for Android. Same purpose as the desktop [Convert-X](../Convert-X) — drop files, pick a format, done. No ads, no uploads, no tracking.

Built with **Kotlin + Jetpack Compose + Material 3** on top of **FFmpeg** via `ffmpeg-kit`.

## Features

- Convert videos, images, and audio between formats
- Resize images (pixels or percentage, aspect-locked)
- GIF editor with trim, palette tuning, FPS and size targeting
- Batch processing with per-file progress
- Advanced options — resolution, FPS, bitrate, preset, trim, strip audio
- Dark / Light theme with brand purple glow accent
- Output directly to a folder you pick (SAF) or Convert-X's own media folder
- Share / open results straight from the output screen

### Supported formats

| Type  | Formats |
|-------|---------|
| Video | MP4, MKV, AVI, WebM, MOV, GIF, FLV, WMV, TS |
| Image | PNG, JPG, WebP, BMP, TIFF, ICO |
| Audio | MP3, WAV, FLAC, OGG, AAC, WMA, M4A, Opus |

## Requirements

- **Android Studio** Ladybug (2024.2) or newer
- **JDK 17** (bundled with recent Android Studio)
- **Android SDK Platform 34** (compile) + **Build Tools 34.0.0**
- Min device: Android 8.0 (API 26). Verified target: Samsung Galaxy S21 FE (Android 13/14)

## Build & run

### In Android Studio

1. Open this folder (`Android App/`) as a project.
2. Let Gradle sync — it downloads FFmpegKit (~120 MB) on first sync.
3. Plug in your S21 FE with **USB debugging** enabled.
4. Pick `app` as the run configuration and hit **Run ▶**.

### Signed release APK

1. Create a keystore:
   ```
   keytool -genkey -v -keystore convertx.keystore -alias convertx -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Copy `keystore.properties.example` to `keystore.properties` at the project root and fill in your values.
3. Build:
   ```
   ./gradlew :app:assembleRelease
   ```
4. APK lands at `app/build/outputs/apk/release/app-release.apk`.

### Install on device via adb

```
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Project layout

```
app/src/main/java/com/cedrickgd/convertx/
  MainActivity.kt              ← Compose host + edge-to-edge setup
  ConvertXApplication.kt       ← Application class, FFmpeg init
  data/
    AppContainer.kt            ← manual DI
    SettingsRepository.kt      ← DataStore-backed settings
  domain/
    Format.kt                  ← catalog of supported formats
    Models.kt                  ← FileEntry, AppSettings, ConvertOptions…
    ConversionEngine.kt        ← engine interface + ConversionProgress
  engine/
    FFmpegEngine.kt            ← ffmpeg-kit wrapper
    FFArgsBuilder.kt           ← pure option → args translator
    SafIo.kt                   ← SAF / DocumentFile helpers
  ui/
    theme/                     ← colors, typography, ConvertXTheme
    components/                ← reusable Compose primitives
    navigation/                ← NavHost + destinations
    screens/
      home/                    ← dropzone → convert flow
      resize/                  ← resize flow
      output/                  ← results panel
      settings/                ← preferences
  util/Format.kt               ← bytes / duration / name helpers
```

## No network

The app strips `INTERNET` permission at manifest merge. It can't phone home even if it tried.
