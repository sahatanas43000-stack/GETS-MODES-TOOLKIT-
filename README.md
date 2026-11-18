# GETS MODES TOOLKIT

Android all-in-one toolkit built with Kotlin and Jetpack Compose.

**Developer credit: Developed by Anas**

## Offline tools
- JSON formatter (validates real JSON), Base64, URL encode/decode, SHA-256 and multi-hash generator, UUID, regex tester
- JWT decoder (header and payload only, signature is not verified), Unix timestamp converter, color picker (HEX/RGB/HSL)
- Secure password generator
- Text counter, case converter, duplicate line remover, line sorter, word frequency, lorem ipsum, Morse code, number to words (English and Bangla)
- Telegram post formatter (bold/italic/link buttons, preview, saved templates)
- Offline notes (many notes, tags, pins, search, export and import)
- QR code generator and scanner
- PDF tools: image to PDF, merge, split, rotate, PDF to images, add signature/stamp
- Image tools: compressor, resize, crop and rotate, PNG/JPG/WebP convert, watermark, text from image (OCR, Latin text)
- Unit converter, age calculator, date difference, tip and percentage, EMI calculator
- Manual Dress Editor (drag, pinch-zoom and twist; shirt, T-shirt, dress or your own PNG)
- ZIP extractor, app-local file manager and batch rename

## Online tools
- Direct video URL download
- Currency converter (live rates, cached for offline use)
- Social source links (Facebook, Instagram, TikTok) open the original platform/browser; this project does not extract media from those pages.
- Download history for files downloaded into this app's Movies folder

## App features
- Dark mode (follows the phone, or choose Light/Dark)
- English and Bangla interface (home screen and tool names; some tool internals are still English)
- Favorites and recently used tools on the home screen
- Each tool has its own icon and colored category
- Home screen widget and long-press shortcuts (QR scan, Notes, Image to PDF, Currency)

## Requirements
- Android 8.0 (API 26) or newer
- JDK 17
- Android Gradle Plugin 8.7.3
- Gradle 8.9

## Build locally
```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest   # run the unit tests
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## Privacy
See `PRIVACY.md`. Release signing and Google Play steps are in `RELEASE_SIGNING.md`.

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
