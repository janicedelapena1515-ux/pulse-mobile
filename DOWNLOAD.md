# Download Pulse Mobile APK

## Option 1: GitHub Releases (Never 404s Once Pushed)
> **[Open This Repository's Releases Page](../../releases)**

### Why `/releases/latest` returns 404 before your first release
GitHub automatically returns **404** for `/releases/latest` and `/releases/latest/download/...` until at least one release has been published in the repository. Also, if AI Studio's **Push to GitHub** omits `.github/workflows/release-apk.yml` (which happens when a GitHub OAuth token lacks the `workflow` scope), GitHub Actions won't automatically publish a release on push.

### How to publish the APK in your GitHub Releases section
1. In Google AI Studio, open **Settings / Export → Generate APK** to download `app-debug.apk` directly to your device.
2. Push this project to your GitHub repository via AI Studio's **Push to GitHub** button.
3. Either:
   - Run the **Build & Release APK** workflow under your repository's **Actions** tab (if `.github/workflows/release-apk.yml` was pushed), **or**
   - Open your repository's **Releases** page → **Create a new release**, create tag `v1.0.0`, attach `app-debug.apk`, and click **Publish release**.

## Installing on Android
1. Download `pulse-mobile-debug.apk` or `app-debug.apk` to your Android 7.0+ (API 24+) device.
2. Tap the `.apk` file and allow installation from your browser or file manager if prompted.
