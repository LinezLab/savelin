# PostVault code recon (read-only)

Read from the `main` tarball (2026-11-21): `lib/services/metadata.dart`, `image_store.dart`, `backup.dart`, `lib/data/post_db.dart`. No code was run or built. PostVault is MIT licensed.

## `metadata.dart` — request + parsing

- `extractUrl(shared)` takes the first `https?://\S+` match from shared text (handles "caption + link" shares).
- `normaliseUrl`: lowercase-ish host handling (`m.` → `www.`), Instagram keeps scheme+host+path only (drops all query/tracking), other hosts keep only `v` for YouTube, everything else dropped. HTTPS always.
- `fetchMetadata`: plain `http.get(Uri.parse(url))` — **no custom headers except** `User-Agent: facebookexternalhit/1.1` and `Accept-Language: en`. 12 s timeout. No Referer. The Dart http client follows redirects by default; no explicit redirect cap. Requires status 200; otherwise returns an empty record.
- Tags parsed with a case-insensitive regex over `<meta property|name="X" content="Y">`: `og:title`, `og:description` (fallback `description`), `og:image`. Entities decoded manually (`&amp;`, `&#039;`, `&#x..;`, etc.).
- Instagram-specific: `og:title` matches `^(.*?) on Instagram: "(.*)"$` → author + caption. Other sites: caption = `title\ndesc` (or title alone). Everything else keeps `title` as-is.
- Weakness: one request, one UA, no WebView, no proxy, no local fallback tile; non-200/parse failure = bare link.

## `image_store.dart` — local image copy

- Images live in `<app documents>/images`, named `base64url(normalisedUrl).jpg` (no `=` padding). A set of existing names is loaded at init.
- `_download`: plain GET of the image URL, 20 s timeout, rejects non-200 or bodies < 500 bytes. No headers/Referer.
- `writeBytes`: writes `<name>.part` with flush, then renames to `<name>` — atomic, no half-images after a crash.
- `ensure(post)`: if local file exists, done; else download `post.imageUrl`; if that fails, re-run `fetchMetadata(post.url)`, download the fresh `og:image`, and persist the new URL via `setImageUrl`. This is the "expired signed URL" recovery.
- `backfill` loops all posts; `remove` deletes file + in-memory name.

## `backup.dart` — backup format

- A ZIP containing `posts.json` (`{format: 1, posts: [...]}`; ids omitted, URL is the identity) and `images/<name>.jpg` for each stored image.
- Import reads `posts.json`, skips URLs already present, inserts new posts, then restores images whose `nameFor(post.url)` matches an `images/` entry and that are not already on disk. Never overwrites existing posts.

## `post_db.dart` — schema and search

- SQLite (`sqflite`), v3 schema: `collections(name PRIMARY KEY)` and `posts(id AUTOINCREMENT, url UNIQUE NOT NULL, author, caption, image_url, category, tags, summary, note, description, links, saved_at INTEGER)`.
- Tags and links are comma/newline-joined strings, parsed back by split/filter. `savedAt` is epoch millis.
- `exists(url)` uses the unique URL. Inserts ignore conflicts.
- `search(query, category, oldestFirst)`: every whitespace-separated lowercase word must match `lower(caption|author|tags|summary|note|description|category) LIKE '%word%'`. This is substring search, not FTS — our Room FTS4/5 design is stronger.

## What we port, and what we intentionally change

- Port: crawler-style UA on the post page, first URL from shared text, OG tag extraction (`og:title`/`og:description`/`og:image`), author/caption split for Instagram, immediate local image download with atomic rename, refetch-on-expiry, and ZIP backup layout (JSON + images/).
- Change: OkHttp with a 10 s timeout, capped redirects (5), 512 KB body cap, and Jsoup for robust tag/JSON-LD parsing instead of regex. Send no Referer unless a fixture proves one is required. Keep the crawler UA in a user-editable setting.
- Change: on login wall/empty tags/no image, continue down our full resolver chain rather than saving a bare link. Always generate a fallback tile with an "Add image" action.
- Change: Room entities + FTS, WebP (max 1080 px, quality ~80) + blurhash instead of raw JPEG bytes.
- Keep MIT notice in NOTICE for the ported request/parse approach and local-copy pattern.
