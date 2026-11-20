# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**House Hunter** — an Android app that catalogues flats seen while house hunting. Each flat ("entry") carries a GPS position, geotagged photos/videos, and text/audio/video notes. The core model (Collection → Entry → Attachments/Notes with template-defined fields) is deliberately generic so another template (Ideas/Journal) can reuse it.

`docs/PLAN.md` is the source of truth for scope, milestone status and the decisions made at each milestone. Read it before starting work and tick its checkboxes when a milestone item lands. Where it and the code disagree, the code wins — e.g. the `EntrySearch` FTS4 table it describes was dropped in M6 in favour of in-memory search text.

## Commands

Gradle wrapper, JDK 17, Android SDK platform 36 (`local.properties` → `sdk.dir`). From PowerShell use `.\gradlew.bat`; from the Bash tool use `./gradlew`.

```bash
./gradlew assembleDebug                 # build the debug APK
./gradlew test                          # JVM unit tests (src/test)
./gradlew testDebugUnitTest --tests "*FieldResolverTest"        # one unit test class
./gradlew testDebugUnitTest --tests "*EntryFiltererTest.statusAndRating_combine"  # one method
./gradlew connectedAndroidTest          # instrumented tests (needs a device/emulator)
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rupeshbhurke.househunter.core.data.db.MigrationTest
./gradlew lint                          # Android Lint
```

**Fresh checkout** (including a factory worktree): there are no dependencies to install — the Gradle wrapper fetches everything. But `local.properties` is git-ignored, so a bare `git clone` or `git worktree add` does not have it and Gradle fails with "SDK location not found". Either set `ANDROID_HOME` or copy the file in; `factory/checks.sh` does this automatically.

`secrets.properties` (git-ignored, `MAPS_API_KEY=...`) is optional: without it the Secrets plugin falls back to `secrets.defaults.properties`, everything builds, and the map screen shows a notice instead of tiles while place search is disabled. Never commit a real key.

**Validation** — one command, matching `factory/checks.sh`:

```bash
./gradlew --console=plain lintDebug testDebugUnitTest assembleDebug
```

`connectedAndroidTest` is deliberately outside it (needs a device or emulator). Run it by hand before merging anything that touches Room entities, migrations or repositories.

## Architecture

Single `:app` module, package-by-feature under `com.rupeshbhurke.househunter`: `core/{data,media,location,util}` + `feature/*` + `navigation` + `share` + `ui`. MVVM + repositories, Hilt, Coroutines/Flow, Compose Material 3, single Activity.

### The field system — the part to understand first

A flat's fields come from three scopes, merged by `FieldResolver.effectiveFields` where **the first definition of a key wins**:

1. **Template** — `HouseHuntTemplate.fields` in code, stored as JSON on `templates`.
2. **Collection extras** — fields promoted to "all flats", JSON on `collections.extraFieldDefsJson`.
3. **Entry's own** — rows in `entry_field_defs`, that one flat only.

`EntryRepository.addCustomField(addToAll = …)` writes to scope 2 or 3. `HouseHunterApp.onCreate` and `CollectionRepository.create` both call `syncBuiltInTemplates()`, which upserts the in-code template — so **adding a template field needs no database migration**; existing collections pick it up at next launch.

Values live in `field_values` with two typed columns. `FieldType.isNumeric` (NUMBER, MONEY, RATING, DATE) decides whether input lands in `numValue` or `textValue`; `FieldFormat`/`FieldFilter` both branch on the same flag, so a new `FieldType` must be handled in `FieldFormat.display`/`editable`, the edit form, and `FilterSheet`. Dates are UTC-midnight millis (what the Material date picker returns). Money renders with Indian digit grouping, hand-rolled in `FieldFormat.number`.

`EntryRepository.saveEntry` replaces *all* of a flat's values in one transaction (delete-for-entry, then insert); blank or unparsable input is simply dropped, which is how a field gets cleared.

### Search, filter and sort are entirely in memory

`EntryRepository.observeSummaries(collectionId)` combines six Flows into `EntrySummary` rows, each carrying its values map and a pre-built lower-case `searchText` blob (`SearchText.build` — title, status, every field as "label: shown raw", contacts, note text, captions). `EntryFilterer.apply` then filters and sorts those rows in Kotlin, including haversine distance (`Geo.distanceKm`). There is no FTS table and no SQL filtering.

**Anything newly searchable must be added to `SearchText.build`**, or free-text search silently misses it.

`FilterStore` is a `@Singleton` in-memory map of `collectionId → EntryFilter`, shared by the list and map screens so search, filters, sort and the near-search centre stay in step. It is not persisted — filters reset when the process dies.

### Persistence and media files

Room `AppDatabase` is at **version 4** with a chain of `AutoMigration`s and `exportSchema = true`. Schemas are committed under `app/schemas/` and wired in as androidTest assets so `MigrationTest` can open a v1 database. Changing an entity means: bump `version`, add the next `AutoMigration`, commit the new `N.json`, and run `connectedAndroidTest`.

Media bytes live in app-private storage at `filesDir/media/{entryId}/`; the database stores the path relative to `filesDir`. Room's FK cascade only deletes rows, so **file cleanup is explicit** — `EntryRepository.deleteEntry` and `CollectionRepository.delete` (which collects entry ids *before* deleting the collection) call `MediaFiles.deleteEntryFolder`. Attachments with `isNote = true` belong to a note and are excluded from the media strip.

### Navigation

Type-safe Navigation Compose: each destination is a `@Serializable` route class in `navigation/AppNavHost.kt`, and its ViewModel reads arguments via `savedStateHandle.toRoute<SomeRoute>()`. A new screen = route class + `composable<Route>` entry + ViewModel reading `toRoute`. ViewModels expose `StateFlow` via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), …)`, often with a `null` initial value so an empty state does not flash before the first database emission.

`ShareActivity` is a second exported activity handling `SEND`/`SEND_MULTIPLE` for images and video; it copies files in, then launches `MainActivity` with `EXTRA_ENTRY_ID` to land on the flat.

## Conventions

- User-visible strings go in `res/values/strings.xml`; enums (`FieldType`, `EntryStatus`, `ContactRole`) carry a `@StringRes labelRes`. Keep literals out of composables apart from symbols and formatted values.
- Tests split by what they need: pure logic in `src/test` (JVM, plain JUnit), anything touching Room or Android in `src/androidTest`. Instrumented tests build repositories by hand against `Room.inMemoryDatabaseBuilder` — Hilt is not used in tests.
- Comments explain *why* (a non-obvious decision or workaround), not what the code does — match the existing KDoc tone.
- Git (per `docs/PLAN.md`): one branch per milestone, merged to `main` on approval; small commits that each build; short imperative subject plus a body for the why; no force-pushing.

## Software factory

`/factory run` builds every open GitHub issue labeled `factory` through
intake → spec → build → gate → review → approve, one PR per issue. State is in
`factory/jobs/` (gitignored); `factory/bin/job` is its only writer. Checks:
`factory/checks.sh`. Dashboard: `cd factory && python3 -m http.server`, then
open http://localhost:8000/dashboard.html.

Each job works in its own git worktree, a full checkout of the repo at
`.worktrees/<job-id>` (gitignored). A tool run from the repo root that doesn't
read `.gitignore` sees those copies too; exclude `.worktrees/` in its config.

This is an Android project, so two things differ from the factory's defaults:
the `ui` and `ux` reviewers read Compose code instead of driving a browser
(there is no dev server, and the `ports` config goes unused), and the gate
never runs the instrumented tests. The approver escalates anything whose
correctness depends on them.
