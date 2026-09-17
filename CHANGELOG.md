# Changelog

All notable changes to the Kavita extension in this fork are documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions refer to the extension's `versionName` (`1.4.$versionCode`, where
`versionCode = extVersionCode + kmkVersionCode`).

## [Unreleased]

## [1.4.32] - 2026-09-17

### Changed

- Full toolchain migration: Kotlin `1.7.21` → `2.4.20`, Gradle `8.13` →
  `9.7.1`, AGP `8.13.0` → `9.4.0`, `kotlinter-gradle` `3.13.0` → `5.7.0`,
  OkHttp `5.0.0-alpha.11` → `5.5.0`, `kotlinx-coroutines` `1.6.4` → `1.11.0`.
  This unblocks future dependency work that was previously capped by
  Kotlin-compiler-metadata compatibility with `1.7.21` (see the 2026-09-16
  finding below). `kotlinx-serialization`'s runtime library is deliberately
  **not** bumped past `1.4.1` — see Fixed section, this one isn't just a
  compile-time concern.
- AGP 9's built-in Kotlin support means the extension no longer applies
  `kotlin-android`/`kotlin("android")` explicitly in `common.gradle` or the
  `buildSrc` convention plugins (`lib-android`, `lib-multisrc`, `utils`) —
  AGP compiles Kotlin sources on its own now. Custom flat `src/` source-set
  layouts needed an explicit `kotlin.srcDirs` alongside the existing
  `java.srcDirs`, since built-in Kotlin no longer treats them as aliased.
- `kotlinter-gradle` 5.x replaced the old `kotlinter { experimentalRules;
  disabledRules }` DSL (removed since 3.14.0) with ktlint's own
  `.editorconfig`-based configuration; moved our two disabled rules
  (`argument-list-wrapping`, `comment-wrapping`) there.
- Ran the project through kotlinter 5.7's ktlint 1.x formatter (`ktlint
  official` code style), which reformats considerably more aggressively than
  the old 3.13.0/ktlint 0.x it replaces (trailing commas, multi-line
  parameter lists, expression-body functions, `SCREAMING_SNAKE_CASE` for
  `const val`, max 140-char lines). Style-only; no behavior change.

### Fixed

- **Extension failing to load at all** (`ClassCastException:
  kotlinx.coroutines.SupervisorJobImpl cannot be cast to
  kotlin.coroutines.CoroutineContext`, crashing the whole Mihon app on
  startup once it got past extension loading into an actual API call).
  AGP's built-in Kotlin auto-adds `kotlin-stdlib` as an `implementation`
  dependency regardless of the extension's own `compileOnly` declaration
  (the convention this whole ecosystem relies on for the host app to supply
  Kotlin/coroutines/serialization at runtime instead of bundling a second,
  incompatible copy into every extension). The extension ended up shipping
  its own `kotlin.coroutines.*` classes alongside Mihon's, and any object
  crossing that boundary (e.g. combining a host-provided `SupervisorJob()`
  with our own `CoroutineContext` interface) threw. Fixed by setting
  `kotlin.stdlib.default.dependency=false` in `gradle.properties`.
- **Login crashing the whole app** (`ClassNotFoundException:
  kotlinx.serialization.internal.GeneratedSerializer$-CC`) after the above
  fix. Unlike coroutines, `kotlinx-serialization`'s RUNTIME library (as
  opposed to its Gradle compiler-plugin artifact, which is correctly pinned
  to the Kotlin version) isn't just compile-time-metadata-gated — since it's
  also `compileOnly` and resolved from whatever Mihon itself bundles at
  runtime, our code must not reference APIs newer than what the actually
  installed host app ships. `1.11.0`'s generated serializers reference a
  default-method desugaring class (`GeneratedSerializer$-CC`) that doesn't
  exist in Mihon's older bundled copy. Reverted the runtime library version
  (not the Gradle plugin, which stays matched to Kotlin `2.4.20`) back to
  the last confirmed-working `1.4.1`.
- `Bad request`-class Kotlin compiler warnings surfaced once the toolchain
  actually compiled the module for real (earlier attempts were silently
  producing empty/`NO-SOURCE` dex output — see above): a redundant
  `.toString()` on an already-`String` parameter, a dead `?: default` on two
  properties that can't actually be null, an unreachable `else` branch in a
  `when` already narrowed by an outer boolean condition, and six overrides
  of now-deprecated `Observable`-returning `HttpSource` methods
  (`fetchPopularManga`, `fetchLatestUpdates`, `fetchSearchManga`,
  `fetchChapterList`, `fetchMangaDetails`, `fetchPageList`) — suppressed
  rather than migrated, since moving to the suspend-based replacements is a
  separate, larger effort.
- `.github/workflows/create_release.yml`'s `target_commitish: repo` pointed
  releases at the orphan `repo` publishing branch instead of the commit
  actually being released, which broke `git describe --tags` for the
  changelog-since-last-tag lookup and made every release fall back to "last
  20 commits" regardless of how recent the previous tag was. Changed to
  `${{ github.sha }}`.

### Dependencies (2026-09-16)

Revisited the dependency audit from the previous session. Bumped `jsoup`
(1.15.3 → 1.23.2) and `commons-text` (1.14.0 → 1.15.0) — pure-Java libraries,
no compatibility concerns. `kotlinx-serialization` got a trivial patch bump
(1.4.0 → 1.4.1, still targeting Kotlin 1.7.x).

**Finding:** `kotlinx-coroutines`, `kotlinx-serialization`, and `OkHttp` are
NOT independently upgradeable the way the original audit assumed — all three
embed Kotlin compiler metadata, and any release built with Kotlin newer than
our pinned `1.7.21` fails with "Module was compiled with an incompatible
version of Kotlin" (confirmed for OkHttp 5.5.0 = Kotlin 2.1.0 metadata, and
kotlinx-serialization/coroutines 1.11.0 = Kotlin 2.3.0 metadata). Our current
pins (OkHttp `5.0.0-alpha.11`, coroutines `1.6.4`) are already at the
practical ceiling for Kotlin 1.7.21. Meaningfully moving any of these three
requires the Kotlin 2.x migration first, not a standalone task.

## [1.4.31] - 2026-09-16

### Fixed

- Dynamic Cover Updates never actually updating covers on already-in-library
  manga (always showed the volume-1-era static cover). `mangaDetailsParse` has
  two branches depending on whether the series is present in the extension's
  in-memory browse/search cache; the per-volume cover selection logic only
  existed in the cache-hit branch. A routine background library refresh
  (the primary real use case for this setting) almost never has the series
  cached, so it silently took the other branch, which never touched
  `thumbnail_url` at all. Extracted the selection logic into a shared
  `resolveThumbnailUrl()` and call it from both branches. Verified live: the
  previously-silent branch now runs the volume lookup and resolves an actual
  per-volume cover URL.

### Changed

- Login now uses a client with redirect-following disabled, so a 3xx response
  (e.g. from a reverse proxy redirecting http->https) surfaces as a clear
  "server redirected this request" error with the target URL, instead of
  OkHttp silently downgrading the POST to GET on the redirect and failing
  with an opaque 404/"Authentication Failed" further down the line.

## [1.4.29] - 2026-09-16

### Fixed

- Series details silently failing to fetch Kavita+ average-score/recommendation
  data (caught internally, falls back gracefully, but logged an error and
  wasted a request every time). `SeriesDetailPlusWrapperDto.ratings` was a
  non-nullable `List<RatingDto>` with a `= emptyList()` default; non-K+-licensed
  servers return this field as a literal JSON `null` rather than omitting it or
  sending `[]`, which a missing-key default can't absorb. Made it nullable.

## [1.4.28] - 2026-09-16

### Fixed

- Reading list thumbnails returning a blank/broken image whenever the list
  had a custom cover set. `readingListParse` and the reading-list details
  path both built a nonexistent `/api/image/{filename}` URL; Kavita only
  exposes fixed routes like `readinglist-cover`, which already handled both
  the custom-cover and no-cover cases correctly. Switched both spots to use
  it unconditionally. (Fixes upstream [Kareadita/tach-extension#59](https://github.com/Kareadita/tach-extension/issues/59))

## [1.4.27] - 2026-09-15

### Fixed

- `SortOptions.sortField`/`isAscending` are now force-serialized via
  `@EncodeDefault(Mode.ALWAYS)`. kotlinx.serialization's default
  `encodeDefaults = false` silently drops a field whose value matches its
  declared default, which is what actually caused the `SortField is
  invalid` 400s in the first place (the 1.4.26 fix worked, but only by
  accident, since `UserRating` happens to differ from the class default).
  Adopted from upstream PR #62 (rodrigofndz).
- Stopped overwriting `sChapter.url` with a bare `/Chapter/{id}` right
  after `helper.chapterFromVolume()` had already set it correctly
  (including the `?split={fileCount}` marker for chapters spanning
  multiple files), which was silently breaking merged/split-chapter
  recognition. `getChapterUrl()` now also strips that marker so the
  browser-facing URL stays clean. Adopted from upstream PR #55 (Zehkul).

## [1.4.26] - 2026-09-15

### Fixed

- "Popular" feed 400ing with `Bad request - Invalid parameters` on
  self-hosted servers without a Kavita+ license. It sorted by
  `AverageRating`, which Kavita's own API docs it as "Kavita+ Only...
  Not usable for non-licensed users." Switched to `UserRating` (local
  user ratings, available to everyone) and added the enum value.

## [1.4.25] - 2026-09-15

### Fixed

- `popularMangaRequest` and both branches of `searchMangaRequest` posted
  to `/Series/all-v2` (capital S) instead of the working `/series/all-v2`
  used by `latestUpdatesRequest`. Turned out to be a coincidental
  correlation rather than the real cause (see 1.4.27), but normalized for
  consistency regardless.

## [1.4.24] - 2026-09-15

### Fixed

- `FilterV2Dto.id` was a nullable `Int` defaulting to `null`, serialized
  as `"id": null`. Kavita's `SeriesFilterV2Dto.Id` is a non-nullable C#
  `int`, so the server rejected the request outright with a 400 before
  it ever got to routing/filtering. Defaulted to `0` instead.

### Infrastructure

- Repointed `build_push.yml`'s hardcoded `Kareadita/tach-extension`
  references to this fork so the self-hosted repo-publishing workflow
  (`repo` branch, `index.min.json`) works here.
- Bumped every pinned GitHub Action off the deprecated Node 20 runtime
  to their latest Node 24 releases (`actions/checkout`, `setup-java`,
  `gradle/actions/setup-gradle`, `upload-artifact`, `download-artifact`,
  `nrwl/nx-set-shas`, `EndBug/add-and-commit`, `dessant/lock-threads`,
  `keiyoushi/issue-moderator-action`, `softprops/action-gh-release`).
- Added `local.signing.properties` (gitignored) as a fallback signing
  source in `common.gradle`, so release builds can be signed locally
  without exporting env vars.
- Repointed README badges and "Install Kavita Repo" links at this fork.
