# Changelog

All notable changes to the Kavita extension in this fork are documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions refer to the extension's `versionName` (`1.4.$versionCode`, where
`versionCode = extVersionCode + kmkVersionCode`).

## [Unreleased]

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
