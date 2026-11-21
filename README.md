# Pinfold

A local-only Android app: share a link from any app and it appears as a Pinterest-style preview tile. No account, no analytics, no cloud. Liquid-glass UI with a floating pill navigation bar and a full Appearance & Motion settings screen.

> Status: early skeleton. Gradle wrapper, vendored glass module, glass design system, CI workflow and recon notes are in place. Feature screens and the resolver pipeline are being built next.

## Privacy guarantees (must remain true)

1. No account, login, or sign-in with any provider.
2. No analytics, crash reporters, ads, or third-party SDKs that phone home.
3. All data is stored on the device. There is no backend.
4. The only network traffic is fetching pages the user bookmarked (and optionally an image from that page). A Network transparency screen logs every host contacted and why; Offline mode disables fetches entirely.
5. Tracking parameters are stripped from saved URLs.

Android cloud auto-backup is disabled (`allowBackup=false`). Cleartext traffic is denied by default. No cookies are stored.

The app declares `INTERNET`, unlike Artefact: link previews require it. This is compensated for by the transparency log, Offline mode, and the rule that no traffic happens unless a link is being fetched.

## Honest limits

No account-free method can fetch previews for private accounts, login-walled posts, or content a platform deliberately hides from link-preview requests. When a real preview cannot be fetched, Pinfold generates a local fallback tile and offers "Add custom image" / "Refetch image". It does not promise that every link gets a real image. Instagram/Facebook CDN image URLs are signed and expire, so image bytes are downloaded at save time and stored locally — the remote URL is never relied on afterwards.

An optional third-party embed proxy may be offered off by default with a clear warning; it is the only place the privacy promise bends and requires per-platform opt-in.

## Build (GitHub Actions only)

The developer machine is low-end; **never build locally**. Every build, test, lint and packaging step runs in CI (`.github/workflows/android.yml`):

- `check` job: `lintDebug testDebugUnitTest`.
- `build` jobs: one per ABI — `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`, `universal` — using `-Pabi=<abi>`.
- Tagged `v*` builds publish a GitHub release.

The wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) was copied from Artefact and uses Gradle 9.8.2. Versions are pinned in `gradle/libs.versions.toml`. ABI filters only change APK contents when native libraries are bundled; with pure-Kotlin dependencies the APKs are functionally identical, but all five are produced as requested.

Debug-signed APKs from CI are for testing only. To ship signed releases, add the `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` repository secrets; without them the release build falls back to the debug key and must be labelled as such.

## Architecture

Kotlin, Jetpack Compose, Material 3, Navigation Compose, MVVM/UDF, single activity. Modules: `app`, `backdrop` (vendored Kyant0 library), `core-ui-glass`, and planned `core-data`, `core-network`, and `feature-*` modules. Room + FTS for storage, OkHttp + Jsoup for parsing, Coil 3 for images, WorkManager for fetch, AlarmManager for reminders.

See `docs/RECON.md` (Artefact visual/motion findings), `docs/POSTVAULT.md` and `docs/POSTVAULT-RECON.md` (preview reference), and `NOTICE` for licences.
