# PostVault reference study (spec section 8)

Source: https://github.com/Asta39/postvault (MIT, Flutter/Dart, Android, no account, no server).

## Confirmed from README

1. Entry point: Share > PostVault in the source app, or paste a link with +. Returns the user to the source app.
2. Preview source: public link preview metadata (captions, author, image), same data a messaging app reads. No Instagram login/API key/account.
3. Local image copy: image is downloaded and kept on device because Instagram's links expire. Matches spec §2.2 signed-CDN problem.
4. Storage: SQLite on device. Backup/export bundles every post and its picture into one file.
5. Weakness: private accounts / posts with no preview saved as bare link in "Other"; no fallback image method.
6. Coverage: Instagram most tested; TikTok, YouTube, X rely on public previews and are less tested.
7. Optional AI step (do NOT copy by default): captions sent to Groq API — conflicts with our privacy promises. Our version uses on-device keyword/tag suggestions only; any cloud mode must be opt-in with a clear warning.

## Agent must verify by reading the code (read-only, no building)

- `lib/services/metadata.dart`: exact HTTP request details (User-Agent, headers, Referer, redirects, timeouts), requested URLs (post page, embed endpoint, or other), tags/JSON fields parsed for image/caption/author, and fallback order.
- `lib/services/image_store.dart`: image download, resize, naming, saving, stale/missing file handling.
- `lib/services/backup.dart`: backup file format (for our export/import design).
- `lib/data/post_db.dart`: table schema and search approach.
- `.github/` or `android/` build files only if useful for CI ideas (we use Kotlin and our own workflow from §7).

Write a short findings note listing each before coding the resolver, quoting only tiny snippets. MIT-licensed; if code is closely translated, keep the MIT notice in NOTICE.

## How to apply (spec §8.2)

PostVault's method = "read public link preview, immediately keep a local copy of the image" — our resolver steps 2/3 plus the caching rule. Adopt as primary Instagram/TikTok/X path, then add fallbacks PostVault lacks:

1. PlatformResolver implementations (InstagramResolver first), registered in ResolverRegistry keyed by host. Port request pattern from metadata.dart.
2. OkHttp, no cookie jar, no Referer unless proven needed, 10 s timeout, first ~512 KB only, Jsoup parse og:image/twitter:image/og:title/og:description/al:* tags.
3. Parse author handle and caption into separate fields; treat caption as untrusted plain text.
4. Download image in same worker run, decode/re-encode WebP (max ~1080 px), save app-private, store width/height/blurhash. Never rely on remote URL after saving.
5. On login wall/empty tags/no image, continue chain: crawler-identity retry → hidden WebView → opt-in proxy → generated fallback tile with "Preview unavailable, add image".
6. "Refetch image" in details; "Add custom image"; share-an-image-to-attach.
7. Backup/export: one file containing DB + images, importable on another phone (design after backup.dart).
8. Test with recorded HTML fixtures; keep UA editable in Settings; keep resolvers isolated.

## Limits for README (spec §8.3)

No account-free method fetches previews for private posts or content hidden from link-preview requests; PostVault admits this. Our extra fallbacks reduce failures but cannot guarantee a real image for every link.
