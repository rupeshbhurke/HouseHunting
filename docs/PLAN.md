# House Hunter — Android App Plan

## Progress
Legend: ⬜ not started · 🟡 in progress · ✅ done (approved) · ⏸ awaiting approval

| Milestone | Status | Branch |
|---|---|---|
| M0 — Project scaffold | ✅ done | `main` |
| M1 — Collections & entries (template-driven) | ⏸ awaiting approval | `m1-collections-entries` |
| M2 — GPS for the flat | ⬜ | `m2-gps` |
| M3 — Photo & video capture | ⬜ | `m3-capture` |
| M4 — Notes: text, audio, video | ⬜ | `m4-notes` |
| M5 — Import (gallery + share sheet) | ⬜ | `m5-import` |
| M6 — Search, filter & compare | ⬜ | `m6-search-compare` |
| M7 — Map view & "near a location" search | ⬜ | `m7-map-near-search` |
| M8 — Export / share | ⬜ | `m8-export` |
| M9 — Google Drive backup & restore | ⬜ | `m9-drive-backup` |

---

## Context
While house hunting it is hard to keep track of each flat you see. Your own photos and videos, plus the ones agents send on WhatsApp, end up mixed together in the gallery with nothing tying them to a specific flat. **House Hunter** gives each flat its own entry. The entry stores the flat's GPS location. Photos and videos are captured or imported straight into it, and each one is geotagged and timestamped. You can also add text, audio and video notes. The core is generic (Collection → Entry → Attachments/Notes, with fields defined by a template), so a later "Ideas / Journal" template can reuse the same app without a rewrite.

**Decisions already made:** Kotlin + Jetpack Compose · on-device storage + Google Drive backup · share sheet + gallery import · generic core with the House template first · per-item geotagging · flat fields: Basics, Money, Contacts, Evaluation · v1 includes map view, compare, search/filter, and export · every field is optional, and you can add custom fields per flat (optionally promoted to every flat in the collection) · every field (base, extra, custom) is searchable · "near a location" search (pick on map, search a place, current location, saved reference points such as Office or School).

**Working mode:** the project is built one milestone at a time. After each milestone the work is shown for approval before the next one starts.

---

## Git workflow
- **Remote:** `https://github.com/rupeshbhurke/HouseHunting.git` (public repo).
- **Secrets never get committed.** The Maps/Places API key and OAuth config go in `secrets.properties` (git-ignored), read through the Secrets Gradle plugin. `secrets.defaults.properties` holds placeholder values. `local.properties` is git-ignored too.
- **Small, logical commits** inside each milestone, each one building cleanly. Messages use a short imperative subject plus a body explaining *why* when it isn't obvious.
- **Branching:** one branch per milestone. Once a milestone is approved, its branch is pushed and merged into `main`.
- No force-pushing or rewriting pushed history.

## Architecture
- **Language/UI:** Kotlin, Jetpack Compose, Material 3, single Activity, Navigation Compose
- **Pattern:** MVVM + repository, Hilt for dependency injection, Coroutines/Flow
- **Persistence:** Room (SQLite). Media files live in app-private storage at `files/media/{entryId}/`
- **Media:** CameraX (photo + video), MediaRecorder (audio), Media3/ExoPlayer (playback), Coil + coil-video (thumbnails)
- **Location:** Fused Location Provider; Android `Geocoder` for reverse-geocoding the address
- **Maps:** Google Maps Compose + Places SDK autocomplete (needs one Google Cloud API key)
- **Backup:** Google Sign-In (Credential Manager) + Drive REST API (`drive.file` scope), WorkManager for scheduled backups
- **SDK levels:** minSdk 26 (Android 8), targetSdk 35, compileSdk 36
- **Package layout (single app module, package-by-feature):**
  `core/data` (Room, repositories) · `core/media` · `core/location` · `feature/collections` · `feature/entry` · `feature/capture` · `feature/notes` · `feature/map` · `feature/compare` · `feature/export` · `feature/backup`

## Data model (generic core)
| Table | Key columns |
|---|---|
| `Template` | id, name, `fieldDefs` (JSON: key, label, type [text/number/money/phone/enum/rating/date], group) — the built-in "House Hunt" template is seeded |
| `Collection` | id, name (e.g. "Pune Hunt 2026"), templateId, `extraFieldDefs` (JSON: fields promoted to "all flats" in this collection), createdAt |
| `EntryFieldDef` | entryId, key, label, type — custom fields that belong to a single flat only |
| `ReferencePoint` | id, collectionId?, name (Office, Kids' school), lat, lng |
| `Entry` | id, collectionId, title, lat, lng, accuracyM, status, rating, createdAt, updatedAt (the address is the template field `address`) |
| `FieldValue` | entryId, fieldKey, textValue, numValue (typed columns so filters and compare can query them) |
| `Contact` | id, entryId, role (Agent/Owner), name, phone |
| `Attachment` | id, entryId, kind (PHOTO/VIDEO/AUDIO), filePath, mime, lat, lng, capturedAt, source (CAPTURED/IMPORTED), caption, durationMs |
| `Note` | id, entryId, kind (TEXT/AUDIO/VIDEO), text, attachmentId?, lat, lng, createdAt |
| `EntrySearch` (FTS4) | entryId, content — every searchable string for an entry |

Fields in the House Hunt template:
- **Basics:** address, society, BHK, floor, area sq ft, facing
- **Money:** rent/price, deposit, maintenance, brokerage
- **Evaluation:** pros, cons, visit date (status and rating are columns on `Entry`)

**Field rules**
- Every field is optional. Only the title is auto-filled (e.g. "Flat 3 – 15 Nov"), and you can edit it.
- Empty fields are hidden on the entry view. The edit form shows them collapsed per group.
- **+ Add field** on a flat creates a custom field (label + type) for that flat only. An **"Add to all flats"** toggle promotes it into the collection's `extraFieldDefs`, so it appears (empty, optional) on every flat and can be used in filters and compare.
- Effective fields for a flat = template fields + collection extras + that flat's own fields.

**Search covers every field**
- **Free-text search:** the FTS4 table indexes title, address, *all* field values (base, collection extras, and per-flat custom fields, stored as "label: value"), contacts, note text and media captions. The repository keeps it in sync on every save.
- **Structured filters on any field:** number/money → min–max range, enum/status → choice chips, rating → minimum stars, date → date range, text/phone → "contains". These run against the typed `FieldValue` columns. Flats that don't have the field don't match that filter.
- Filters, free text and distance search all combine.

**Distance search**
- Haversine distance. A Room query pre-filters by a lat/lng bounding box, then results are filtered exactly and sorted by distance in Kotlin.

---

## Milestones

### M0 — Project scaffold ✅
- [x] Gradle project with a version catalog (AGP 8.13, Kotlin 2.2, Gradle 8.14.3 wrapper)
- [x] Compose + Material 3, Hilt, Room/KSP, kotlinx-serialization, and Secrets plugin configured
- [x] App theme, Hilt `Application`, `MainActivity`, navigation shell with home screen
- [x] `.gitignore`, `secrets.defaults.properties`, README
- [x] `assembleDebug` + `testDebugUnitTest` pass
- [x] Git init, first commits pushed to `origin/main`

> Note: Room's `@Database` class arrives with the first entities in M1 (Room needs at least one entity to compile).

### M1 — Collections & entries (template-driven) ⏸
- [x] Room entities/DAOs for the generic core + database class
- [x] Seed House Hunt template (upserted from code when a collection is created)
- [x] Collections list: create, rename, delete
- [x] Entry list cards (title, BHK, price, status, rating; photo thumbnail comes with M3)
- [x] Entry detail + edit form from the effective field list (all optional, blanks hidden)
- [x] + Add field per flat, with "Add to all flats"
- [x] Contacts with tap-to-call / WhatsApp
- [x] Unit tests for field resolution/formatting; instrumented repository tests (in-memory Room)
- [x] Instrumented tests (10) run green on an emulator
- [x] Manual walkthrough on an emulator: create collection, add flat, set status/rating/price, add custom field to all flats, add contact, call button opens the dialer

> Decisions made in M1: Basics has "Area (sq ft)" (number) plus "Area type" (Carpet, RERA Carpet, Built-up, Super built-up). `address` is a template field (stored as a field value) rather than an `Entry` column, so M2's reverse-geocoded address will be written into that field. Amounts display with ₹ and Indian digit grouping. WhatsApp links assume +91 for 10-digit numbers.

### M2 — GPS for the flat ⬜
- [ ] Location permission flow
- [ ] Fused location fix with accuracy
- [ ] Reverse-geocoded address
- [ ] Manual pin adjust/override
- [ ] "Open in Maps" link

### M3 — Photo & video capture ⬜
- [ ] CameraX screen launched from an entry (photo + video)
- [ ] Per-capture lat/lng + timestamp, saved to `files/media/{entryId}/`
- [ ] Entry media gallery: grid, full-screen viewer, video playback, caption, delete

### M4 — Notes: text, audio, video ⬜
- [ ] Notes timeline in the entry
- [ ] Text note editor
- [ ] Audio recorder + playback
- [ ] Video note (CameraX video mode)
- [ ] Every note geotagged + timestamped

### M5 — Import (gallery + share sheet) ⬜
- [ ] Photo Picker import inside an entry
- [ ] Share target for `image/*`, `video/*` (single + multiple)
- [ ] Choose collection/entry or create a new one from the share flow
- [ ] Copy into app storage; read EXIF GPS/date

### M6 — Search, filter & compare ⬜
- [ ] FTS index over every field (base + extra + custom), notes, contacts, captions
- [ ] Dynamic filter sheet for any field type
- [ ] Compare 2–3 entries side by side with cover photos

### M7 — Map view & "near a location" search ⬜
- [ ] Map pins colored by status, preview card on tap
- [ ] Center point: long-press/drag pin, Places search, current location
- [ ] Radius slider (0.5–20 km) with circle; list sorted by distance; combines with filters
- [ ] Saved reference points + distance on cards/detail, sort by distance to a point
- [ ] *You:* Google Cloud API key with Maps SDK for Android + Places API (New)

### M8 — Export / share ⬜
- [ ] PDF summary (fields, contacts, thumbnails, notes, map link)
- [ ] ZIP (PDF + all media)
- [ ] Share via Android share sheet

### M9 — Google Drive backup & restore ⬜
- [ ] Google sign-in, `drive.file` scope
- [ ] Back up DB snapshot + incremental media to a "House Hunter Backup" folder
- [ ] Manual + scheduled (Wi-Fi only) backups via WorkManager
- [ ] Restore on a new device
- [ ] *You:* Google Cloud OAuth client + Drive API enabled

### Later (not in v1)
"Ideas / Journal" template (same core, different fields), tags, multi-device sync.

---

## Verification
- **Each milestone:** `./gradlew assembleDebug` and `./gradlew test` (repository/DAO tests with an in-memory Room database).
- **Instrumented/UI tests:** `./gradlew connectedAndroidTest` for the entry form and navigation, on the emulator.
- **Manual end-to-end test on a phone:** create an entry at a location → capture photos, video, audio note → check per-item location/time → share a WhatsApp image into the entry → add a custom field and promote it → search by a custom field (text + filter) → filter and compare → map pins → radius search from a searched place → export PDF/ZIP → Drive backup, clear data, restore.
- **Emulator mock locations** for testing GPS.
- **Prerequisites:** Android Studio, JDK 17+, Android SDK (here: `C:\Android\Sdk`), a device or emulator.
