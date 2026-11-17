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
- Offline notes
- ZIP extractor
- Image compressor and resize
- Manual Dress Editor with a movable, resizable, recolorable shirt overlay
- App-local file manager and batch rename

## Online tools
- Direct video URL download
- Social source links (Facebook, Instagram, TikTok) open the original platform/browser; this project does not extract media from those pages.
- Download history for files downloaded into this app's Movies folder

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
- Internet permission is used only by the optional online downloader.
- Files created by the app are stored in app-specific storage.
- No login, analytics, ads, AI service or cloud backend is included.
- ZIP extraction rejects entries that attempt to write outside the extraction folder.

## Dress Editor limitation
The Manual Dress Editor places a basic shirt-shaped overlay on top of a selected photo. It supports moving, scaling and recoloring the overlay. It is not AI virtual try-on, does not automatically detect body shape, and does not remove or replace the original clothing. For a polished editor, add original transparent PNG clothing assets and more precise touch/rotation controls.

## Video download limitations
Only direct downloadable video-file URLs are downloaded by Android DownloadManager. Facebook, Instagram and TikTok links open the source page; extracting video from social pages is not implemented. Do not download content without permission.
