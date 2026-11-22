# Pulse Mobile — Luxury Cherry Red Edition

A Kotlin & Jetpack Compose Android device optimization, real-time telemetry, gaming profile, and Storage Access Framework (SAF) cleaner application.

## Download APK

Once this repository is pushed to GitHub:

1. **GitHub Releases (Direct Download Link):**  
   Open the **Releases** section on the right sidebar of this GitHub repository (or visit `https://github.com/<YOUR_USERNAME>/<YOUR_REPO>/releases/latest`) to download the latest `pulse-mobile-debug.apk`.
   - You can publish a new APK release automatically by pushing a tag (`git tag v1.0.0 && git push origin v1.0.0`) or by running the **Build & Release APK** workflow manually from the **Actions** tab (`workflow_dispatch`).
2. **GitHub Actions Artifacts:**  
   Every push to `main` runs `.github/workflows/release-apk.yml` and uploads `pulse-mobile-debug.apk` as a downloadable build artifact in the **Actions** tab.
3. **AI Studio Direct Export:**  
   In Google AI Studio, you can also generate and download the APK directly from the **Settings / Export** menu.

## Highlights

- **Home Dashboard & Pulse Score:** Transparent 0–100 score computed from live storage headroom, RAM availability, battery thermals, and system readiness.
- **Real Boost Engine:** Safe app-cache cleanup, SAF folder auditing, honest Android Low Memory Killer guidance, and shortcuts to Android system storage, app management, battery, display, and developer animation settings.
- **Smart Storage Cleaner:** Zero-permission Storage Access Framework (`OpenDocumentTree`) scanner with image previews, size/date sorting, multi-select deletion, SHA-256 duplicate detection, and Photo Picker media inspection.
- **Gaming Boost:** Room-persisted Game Library, `BALANCED` / `PERFORMANCE` / `BATTERY SAVER` profiles (with real DND and brightness control when permitted), live session timer, and session statistics.
- **Real-Time Monitor:** Live charts for RAM, network throughput (`TrafficStats`), battery temperature, CPU frequency scaling (clearly marking OS-restricted fields), uptime, and detected sensors.
