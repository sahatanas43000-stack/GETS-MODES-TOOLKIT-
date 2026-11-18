# Signed release build (optional)

By default GitHub Actions builds a **debug** APK. Debug builds work fine, but Android's Play Protect may warn about them.
A **signed release** build uses your own private key and is the format Google Play needs.

The workflow switches to a signed release build automatically when four repository secrets exist.

## 1. Create a keystore (once, on a computer with Java)
```bash
keytool -genkeypair -v -keystore gets-release.jks -alias gets \
  -keyalg RSA -keysize 2048 -validity 10000
```
Remember the passwords you type. **Back up `gets-release.jks` somewhere safe and never commit it.**
If you lose it you can never update the same app on Google Play.

## 2. Turn the keystore into text
```bash
base64 -w0 gets-release.jks        # Linux
base64 -i gets-release.jks         # macOS
```

## 3. Add repository secrets
GitHub repository > **Settings > Secrets and variables > Actions > New repository secret**

| Secret name         | Value                                  |
|---------------------|----------------------------------------|
| `KEYSTORE_BASE64`   | the base64 text from step 2            |
| `KEYSTORE_PASSWORD` | the keystore password                  |
| `KEY_ALIAS`         | `gets` (or the alias you chose)        |
| `KEY_PASSWORD`      | the key password                       |

## 4. Push to `main`
The workflow then builds `assembleRelease` and `bundleRelease`, and the **Releases > Latest build** page gets:
- `GETS-MODES-TOOLKIT.apk` (signed, for direct install)
- `GETS-MODES-TOOLKIT.aab` (App Bundle, the upload format for Google Play)

## Publishing on Google Play (summary)
1. Create a Google Play Console developer account (one-time registration fee).
2. Create the app, then upload the `.aab` file.
3. Use `PRIVACY.md` as the basis for your privacy policy page (it must be hosted at a public URL, for example GitHub Pages).
4. Fill in the Data safety form honestly: the app collects no personal data itself. Mention the camera scanner (Google Play services) and the currency-rate request.
5. Add screenshots, a short description and a 512x512 icon.

Note: the repository secrets are only available to workflows on this repository, not to pull requests from forks.
