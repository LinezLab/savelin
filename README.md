# Sonil

A local-only Android app: share a link from any app and it appears as a Pinterest-style preview tile. No account, no analytics, no cloud, no tracking. Liquid-glass UI with a floating pill navigation bar and a full Appearance & Motion settings screen.

The launcher icons and notification glyph were supplied by the project owner; see [NOTICE](NOTICE) for attribution.

## What it does

- **Share to save.** Android's share sheet sends `text/plain` links (plus captions) straight into Sonil. Paste a link with **+** as well. The first URL in shared text is extracted, normalised and de-duplicated.
- **Real previews when possible.** A Shelf-style direct fetch (WhatsApp then desktop User-Agent, 5s timeouts, text/html only) is tried FIRST for metadata and the image. If it cannot supply an image, the current Open Graph / Twitter-card mobile and crawler retries run. Instagram author/caption parsing follows PostVault. YouTube and Reddit have fast paths. See [docs/RECON-SHELF.md](docs/RECON-SHELF.md).
- **Images stay on the device.** The preview image is downloaded, re-encoded as WebP (max 1080 px), stored in app-private storage with a blurhash, and never referenced by its expiring remote URL again.
- **Graceful fallbacks.** If a login wall or empty tags block the preview, Sonil shows a generated tile with **Refetch image** and **Add custom image** actions. An opt-in proxy template is available in Settings for the hard cases.
- **Organise.** Important, archived, tags, collections, notes, search (SQLite FTS), and reminders with notifications.
- **Appearance & Motion.** Five glass presets, five motion styles, transition picker, animation speed, haptics, hold-to-preview delay/effect, reduce-transparency/reduce-motion, low-end device mode and an FPS overlay.
- **Privacy controls.** Offline mode, a network-transparency log of every host contacted, biometric lock, and ZIP backup/export/import that keeps your data on your device.

> Honest limit: no account-free method can fetch previews for private accounts, login-walled posts, or content a platform deliberately hides from link-preview requests. Sonil reduces failures with extra fallbacks and manual images, but cannot guarantee a real image for every link.

## Privacy guarantees

1. No account is required. There is no sign-in and no cloud sync.
2. No analytics, crash reporters, ads, or third-party SDKs that phone home.
3. All data is stored on-device. There is no developer-run server and no cloud copy.
4. The only network traffic is fetching pages you save (and optionally an image from that page). Every host is logged in **Settings → Network transparency**; **Offline mode** disables fetches entirely.
5. Tracking parameters (`utm_*`, `fbclid`, `igsh`, …) are stripped from saved URLs.
6. Android cloud auto-backup is disabled (`allowBackup=false`), cleartext traffic is denied, no cookies are stored, and redirects are capped.

`INTERNET` is declared because link previews require it. This is compensated for by the transparency log, Offline mode, and the rule that no traffic happens unless a link is being fetched. An optional third-party proxy is off by default with a clear warning.

## Architecture

Kotlin, Jetpack Compose, Material 3, MVVM/UDF, single activity. Modules:

| Module | Purpose |
| --- | --- |
| `app` | Screens, share target, WorkManager workers, alarms, backup, DI-free ViewModels |
| `backdrop` | Vendored Kyant0 liquid-glass backdrop library (Apache-2.0) |
| `core-ui-glass` | Glass design system, motion engine, aurora wallpaper, tabs, buttons, overlays |
| `core-data` | Room entities/DAOs/FTS, DataStore settings, repositories |
| `core-network` | URL normaliser, platform detection, resolvers, image store + blurhash |

Storage is Room + FTS4; settings are DataStore Preferences; images are WebP files in app-private storage. Fetching runs in WorkManager; reminders use AlarmManager + notifications.

## Build (GitHub Actions only)

The developer machine is low-end; **never build locally**. Every lint, test and packaging step runs in [`.github/workflows/android.yml`](.github/workflows/android.yml):

- `check` — `lintDebug testDebugUnitTest`.
- `build` — a single universal release APK (`-Pabi=universal`). The build retries transient Maven 429s.
- `release` — on `v*` tags only, publishes the universal APK with a generated release body containing its size, SHA-256 checksum, and a grouped changelog.

The Gradle wrapper (Gradle 9.8.2) and all dependency versions are pinned in `gradle/libs.versions.toml`. Compile SDK 37, min SDK 29, Java 21 in CI (toolchain 17 bytecode).

### Signing

Release builds are signed in CI with a keystore supplied through repository secrets:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | base64 of the release `.jks` |
| `KEYSTORE_PASSWORD` | store password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

The keystore itself lives in the untracked `secrets/` directory and is **never committed**. Without these secrets the release build fails, unless an unsigned test build is explicitly requested with `workflow_dispatch` and `allow_unsigned`; such a build is not suitable for distribution.

### Google Drive sync (removed)

Google Drive sign-in and sync have been removed in this version. Local ZIP backup, export and import remain available in Settings.

## Install

Download the universal APK from the Releases page; it works on every supported device. Verify with `sha256sum savelin-*.apk` against the checksum in the release notes.

## Licences and references

See [`NOTICE`](NOTICE) and [`third_party/`](third_party/) for Artefact (MIT/Apache-2.0), AndroidLiquidGlass (Apache-2.0), PostVault (MIT), Shelf (GPL-3.0, method study only) and Android-Link-Preview (MIT, attribution only) licences. Design and motion findings are in [`docs/RECON.md`](docs/RECON.md); the PostVault study is in [`docs/POSTVAULT.md`](docs/POSTVAULT.md) and [`docs/POSTVAULT-RECON.md`](docs/POSTVAULT-RECON.md); the Shelf study is in [`docs/RECON-SHELF.md`](docs/RECON-SHELF.md).
