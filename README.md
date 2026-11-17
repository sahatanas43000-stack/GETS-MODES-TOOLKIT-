# GETS MODES TOOLKIT

Android all-in-one toolkit built with Kotlin and Jetpack Compose.

**Developer credit: Developed by Anas**

## Offline tools
- JSON formatter (validates real JSON)
- Base64 encode/decode
- URL encode/decode
- SHA-256 hash
- Hash generator (MD5, SHA-1, SHA-256, SHA-512)
- JWT decoder (header and payload only, signature is not verified)
- Unix timestamp converter
- Secure password generator
- Line sorter
- UUID generator
- Regex tester
- Text counter and case converter
- Duplicate line remover
- Telegram post formatter
- Offline notes (multiple notes, search)
- QR code generator and scanner
- Image to PDF and PDF merge
- Unit converter
- ZIP extractor
- Image compressor and resize
- Manual Dress Editor with a movable, resizable, recolorable shirt overlay
- App-local file manager and batch rename

## Online tools
- Direct video URL download
- Currency converter (live rates)
- Social source links (Facebook, Instagram, TikTok) open the original platform/browser; this project does not extract media from those pages.
- Download history for files downloaded into this app's Movies folder

## What's new in 2.3.0
- New app logo and launcher icon.
- Redesigned home screen: tools grouped by category, each with its own colored icon.

## What's new in 2.2.0
- QR code generator and scanner (camera scan uses Google Play services; image scan is fully offline)
- Image to PDF and PDF merge (share/save through the Android share sheet)
- Offline Notes now supports many notes with search (old single note is migrated)
- Unit converter (offline) and currency converter (live rates, cached for offline use)
- Each push to `main` also publishes the APK under Releases > Latest build

## Download the app
1. Open the repository on GitHub, go to **Releases** and open **Latest build**.
2. Tap `GETS-MODES-TOOLKIT.apk` to download it.
3. Open the file on your phone. If Android asks, allow "Install unknown apps" for your browser.
4. This is a debug build, so Play Protect may show a warning. Choose "Install anyway" only for your own build.

Alternative: **Actions** > latest run > **Artifacts** > `gets-modes-toolkit-debug-apk` (a ZIP that contains the APK).

## What's new in 2.1.0
- Fixed a compile error on the home screen footer.
- JSON formatter now uses a real parser instead of bracket counting.
- Batch Rename can no longer overwrite files.
- Dress Editor saves the overlay exactly where it was placed on screen.
- Large photos are downsampled and EXIF rotation is respected (no more out-of-memory crashes or sideways photos).
- ZIP extractor has file-count and size limits.
- Download file names are sanitised and timestamped.
- New tools: hash generator, JWT decoder, timestamp converter, password generator, line sorter.

## Requirements
- Android 8.0 (API 26) or newer
- JDK 17
- Android Gradle Plugin 8.7.3
- Gradle 8.9

## Build locally
```bash
gradle :app:assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions
Push to the `main` branch. GitHub Actions builds a debug APK. Download it from the workflow run's Artifacts.

## Storage and privacy
- Offline tools do not make network requests.
- Internet permission is used only by the optional video downloader and currency rates.
- Files created by the app are stored in app-specific storage.
- No login, analytics, ads, AI service or cloud backend is included.
- ZIP extraction rejects entries that attempt to write outside the extraction folder.

## Dress Editor limitation
The Manual Dress Editor places a basic shirt-shaped overlay on top of a selected photo. It supports moving, scaling and recoloring the overlay. It is not AI virtual try-on, does not automatically detect body shape, and does not remove or replace the original clothing. For a polished editor, add original transparent PNG clothing assets and more precise touch/rotation controls.

## Video download limitations
Only direct downloadable video-file URLs are downloaded by Android DownloadManager. Facebook, Instagram and TikTok links open the source page; extracting video from social pages is not implemented. Do not download content without permission.
