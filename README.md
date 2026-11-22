# ToonMotion Studio

Professional mobile 2D cartoon animation studio built with Kotlin, Jetpack Compose, Material Design 3, Room, DataStore, and Android `MediaCodec`/`MediaMuxer`.

## Build on GitHub (GitHub Actions)

This repository includes `.github/workflows/android-build.yml` so you can build installable **APK** and **AAB** packages directly on GitHub:

1. Push this project to your GitHub repository (`main` or `master` branch).
2. Open the **Actions** tab on GitHub — the **Build Android APK & AAB** workflow runs automatically (or click **Run workflow** manually anytime).
3. Download your built files from the workflow run's **Artifacts** section:
   - `ToonMotion-Studio-Debug-APK`
   - `ToonMotion-Studio-Release-APK`
   - `ToonMotion-Studio-Release-AAB`

### Optional Release Signing Secrets on GitHub
If you want the Release APK/AAB signed with your own Play Store upload keystore instead of a temporary CI keystore, add these **Repository Secrets** in GitHub (`Settings -> Secrets and variables -> Actions`):
- `RELEASE_KEYSTORE_BASE64`: Base64-encoded `.jks` keystore (`alias: upload`)
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: Key password

## Build Locally

```bash
# 1. Decode the included debug keystore (first time only)
base64 -d debug.keystore.base64 > debug.keystore

# 2. Copy .env.example to .env if needed
cp .env.example .env

# 3. Run unit tests & build Debug APK
./gradlew :app:testDebugUnitTest --tests "com.example.ExampleUnitTest"
./gradlew :app:assembleDebug
```

The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
