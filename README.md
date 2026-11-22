# Pulse Mobile — Luxury Cherry Red Edition

A Kotlin & Jetpack Compose Android device optimization, real-time telemetry, gaming profile, and Storage Access Framework (SAF) cleaner application.

## Download APK

> **[Download Latest APK (GitHub Releases)](../../releases/latest/download/pulse-mobile-debug.apk)**  
> *(Or open the **Releases** tab: [View All Releases](../../releases))*

### How GitHub Releases Work Here
1. Push this project to GitHub (via AI Studio's **Push to GitHub** button).
2. GitHub Actions (`.github/workflows/release-apk.yml`) automatically runs on every push to `main`/`master` (or any `v*` tag), builds `pulse-mobile-debug.apk`, and publishes it under the **`latest`** GitHub Release.
3. Once the workflow finishes (~2–3 minutes in the **Actions** tab), the **Download Latest APK** link above and the **Releases** sidebar on GitHub will download `pulse-mobile-debug.apk` directly.

## Highlights

- **Home Dashboard & Pulse Score:** Transparent 0–100 score computed from live storage headroom, RAM availability, battery thermals, and system readiness.
- **Real Boost Engine:** Safe app-cache cleanup, SAF folder auditing, honest Android Low Memory Killer guidance, and shortcuts to Android system storage, app management, battery, display, and developer animation settings.
- **Smart Storage Cleaner:** Zero-permission Storage Access Framework (`OpenDocumentTree`) scanner with image previews, size/date sorting, multi-select deletion, SHA-256 duplicate detection, and Photo Picker media inspection.
- **Gaming Boost:** Room-persisted Game Library, `BALANCED` / `PERFORMANCE` / `BATTERY SAVER` profiles (with real DND and brightness control when permitted), live session timer, and session statistics.
- **Real-Time Monitor:** Live charts for RAM, network throughput (`TrafficStats`), battery temperature, CPU frequency scaling (clearly marking OS-restricted fields), uptime, and detected sensors.
