# UX/UI Redesign Blueprint — Android Music Downloader (Offline-First)

## 1) Product Positioning (North Star)

**This app is not a streaming player.**  
It is a **premium download operations center** for music.

Core loop:

> **SEARCH → ANALYZE → DOWNLOAD → MANAGE**

Design pillars:
- Fast to act (low friction, high response speed)
- Clear system state at all times
- Dense but readable information
- Offline-first and robust against missing data
- Technical elegance (tool-grade UI, not editorial feed)

---

## 2) Information Architecture (Navigation)

### Bottom Navigation (only 4 tabs)
1. **Home** (control center)
2. **Search** (core entry)
3. **Queue** (operations monitor)
4. **Library** (offline organization)

> Settings moves to top-level overflow / profile icon / modal route, **not** bottom nav.

### Route Graph (Compose Navigation)
- Splash
- MainShell (BottomNav)
  - HomeRoute
  - SearchRoute
  - QueueRoute
  - LibraryRoute
- Secondary routes
  - ResultsRoute(query/type)
  - MediaDetailsRoute(mediaId/source)
  - DownloadConfigRoute(mediaId)
  - BatchConfigRoute(batchId)
  - DownloadDetailRoute(taskId)
  - StorageManagerRoute
  - HistoryRoute
  - SettingsRoute

### Global UX Rule
Any screen can deep-link into Queue and Download Detail from a progress chip in header.

---

## 3) End-to-End UX Flows

## A) Fast URL Flow (Primary Power Flow)
1. User taps **Paste URL** (Home/Search quick action).
2. URL parser panel appears with instant validation state.
3. Metadata prefetch starts (skeleton + source badge).
4. User picks quality/format presets (or one-tap smart preset).
5. Task enters queue with visible state transition animation.
6. Queue provides live progress, speed, ETA, retry, priority.
7. On completion: CTA to “Open in Library” + file location info.

### B) Search Flow
1. Enter query in dominant search field.
2. Results segmented by **Songs / Albums / Playlists**.
3. User opens media details or multi-select quick add.
4. Download config sheet/screen with size estimates.
5. Queue + status feedback.

### C) Library Management Flow
1. Open Library.
2. Filter/sort by type, date, size, source, quality.
3. Bulk actions: move, delete, re-tag, re-scan metadata.
4. Storage insights and cleanup suggestions.

---

## 4) Screen Blueprints

## 4.1 Home = Operations Dashboard

### Structure
1. **Compact header**
   - Service status (idle/downloading/offline)
   - Active tasks count
   - Used storage with compact progress bar
2. **Hero Search Bar** (most prominent element)
   - Placeholder examples: “Search song, album, playlist or paste URL”
3. **Quick Actions grid (2x2 or horizontal chips)**
   - Paste URL
   - Download Album
   - Download Playlist
   - History
4. **Active Downloads module**
   - Top 2–3 running tasks with real progress, speed, ETA
5. **Recent Downloads module**
   - Latest completed with status chips
6. **Storage module**
   - Free/used, cache health, one-tap cleanup

### Anti-patterns explicitly blocked
- No trending feed
- No “Made for you”
- No fake recommendation blocks

---

## 4.2 Search Screen (Full-focus)

### Regions
1. Dominant sticky search field
2. Query mode chips: All / Songs / Albums / Playlists / URL
3. Recent searches and recent URLs
4. Suggestion/history actions: pin, clear one, clear all
5. State container area:
   - Empty intro state
   - Skeleton loading list
   - Error with retry + diagnostics hint

### UX behavior
- Debounced local input + explicit search action
- URL detection in-line with icon/state badge
- Keyboard “Search” submits immediately

---

## 4.3 Results Screen

### Layout
- Top segmented control: Songs | Albums | Playlists
- Each section supports compact list and “View all” if aggregated view

### Result Card System (compact + dense)
Card slots:
- Artwork (fallback gradient/icon)
- Title (2 lines max)
- Subtitle (artist/channel)
- Meta row (duration · year · item count)
- Right actions:
  - Quick Download
  - More (open details, add to batch, copy URL)

### Visual hierarchy
- Typography scale emphasizes title + actionable state
- Metadata uses muted but readable contrast
- Surface tint by state (normal/selected/queued)

---

## 4.4 Media Details

Purpose: final “analyze before download” checkpoint.

### Must include
- Large artwork hero with fallback treatment
- Title + creator + source + reliability indicators
- Metadata chips (duration, track count, explicit, source)
- Expandable technical panel:
  - Available formats
  - Expected size range
  - Metadata completeness score
- Primary CTAs:
  - Download now
  - Configure
  - Add to batch

---

## 4.5 Download Config (Premium panel/screen)

No legacy dialogs.

### Sections
1. Quality selector (segmented + advanced)
2. Format selector (MP3, M4A, FLAC where available)
3. Naming/location preset
4. Toggle options:
   - Embed artwork
   - Fetch lyrics
   - Normalize metadata
5. Estimated output size + estimated time
6. CTA row:
   - Add to Queue
   - Start now (high priority)

---

## 4.6 Queue Screen (Mission-critical)

### Tabbed states inside queue
- Downloading
- Waiting
- Completed
- Failed

### Queue Item (rich operational card)
Shows:
- Artwork
- Title + source
- Progress bar
- Speed (MB/s)
- ETA
- Downloaded / total size
- Quality + format chips
- Current stage:
  - Waiting / Downloading / Converting / Tagging / Completed / Failed / Paused

### Item actions
- Pause/Resume
- Retry
- Prioritize up/down
- Remove
- Open details

### Batch controls (top app bar)
- Pause all / resume all
- Retry failed
- Clear completed
- Optimize order

### Motion
- Smooth progress interpolation
- Stage transition crossfade
- Reordering animation

---

## 4.7 Download Detail Screen

A single-task telemetry screen.

Includes:
- Large artwork + title block
- Timeline of stages
- Live graph-like indicators (speed, remaining size)
- Logs/diagnostics collapsible section
- Recovery actions (retry with fallback format/source)

---

## 4.8 Library Screen (Offline Asset Manager)

### IA inside library
- Tabs or filter chips: Tracks / Albums / Playlists / Files
- Sort: recent, size, artist, name, quality
- Filter: format, source, metadata completeness, download date

### Capabilities
- Multi-select management
- Move/rename/delete
- Rebuild metadata
- Open containing folder
- Detect duplicates

### Storage intelligence
- “Large files” view
- “Incomplete metadata” queue
- “Broken files” repair flow

---

## 5) Design System (Visual + Interaction)

## Color & surfaces
- Avoid pure black backgrounds; use near-black tonal surfaces
- 3-layer elevation system with subtle contrast shifts
- Accent color only for actions/status, not decoration overload

## Spacing & density
- Base 4dp grid
- Standard compact paddings: 12/16dp
- Cards with medium density (not oversized)

## Typography
- Clear hierarchy:
  - Title emphasis for entities
  - Numeric emphasis for progress/speed/ETA
  - Secondary metadata de-emphasized but legible

## Components (reusable)
- `PrimarySearchBar`
- `QuickActionChip`
- `MediaResultCard`
- `QueueTaskCard`
- `StatusBadge`
- `StorageUsageCard`
- `StateContainer` (loading/empty/error)
- `FallbackArtwork`
- `ShimmerBlock`

---

## 6) State Model & Fallback Matrix

## Core states (global)
- Loading
- Empty
- Error
- Retrying
- Offline
- No storage

## Download lifecycle states
- Waiting
- Downloading
- Converting
- Metadata loading
- Paused
- Completed
- Failed

## Fallback rules
1. Missing thumbnail → gradient artwork + file/music icon
2. Missing artist/title → use filename-derived label + “Unknown” chip
3. Missing duration → “--:--” + tooltip “Unavailable metadata”
4. No connection → offline banner + queued retry policy details
5. Duplicate file → conflict resolution sheet (skip/rename/replace)
6. Partial metadata → warning badge + one-tap “Repair metadata”

---

## 7) Motion System (Compose-focused)

Use:
- `animateContentSize` for expandable cards/panels
- `AnimatedVisibility` for state sections
- `Crossfade` for status/content transitions
- Skeleton shimmer for loading lists
- Micro-interactions on taps, selection, and queue state changes

Motion principles:
- 150–250ms for micro transitions
- 250–350ms for container transitions
- Easing: standard + decelerate on entrance
- Never block interaction during animation

---

## 8) Compose Architecture Recommendation

## UI layer structure
- `features/home/`
- `features/search/`
- `features/results/`
- `features/download/` (details/config/queue)
- `features/library/`
- `core/designsystem/`
- `core/ui-state/`
- `core/navigation/`

## State handling
- UDF/MVI style per feature
- Immutable UI state models
- Sealed intents/events
- Side-effects isolated (downloads, parser, storage scans)

## Suggested state classes
- `SearchUiState`
- `ResultsUiState`
- `QueueUiState`
- `DownloadConfigUiState`
- `LibraryUiState`

## Domain models for queue richness
- `DownloadTask`
- `DownloadTelemetry`
- `TaskFailureReason`
- `StorageSnapshot`

---

## 9) Practical Redesign Priorities (Execution Plan)

## Phase 1 — Foundation
- Navigation graph refactor (4-tab shell)
- Design tokens (spacing, colors, elevation, typography)
- Shared state container + shimmer

## Phase 2 — Core loop
- New Home dashboard
- Full Search screen + Results segmentation
- Media details + download config premium flow

## Phase 3 — Queue excellence
- Rich queue cards + task actions
- Download detail telemetry
- Batch controls + prioritization

## Phase 4 — Library + robustness
- Offline library management
- Storage intelligence + duplicates + repair
- Full fallback and error UX hardening

---

## 10) KPI & UX Quality Metrics

Track:
- Time from app open to task queued
- Search-to-download conversion rate
- Queue failure recovery rate
- Average actions to manage completed downloads
- Library task completion time (sort/filter/delete)
- Crash-free sessions during unstable network/storage conditions

---

## 11) What this redesign intentionally avoids

- Streaming-app emotional storytelling
- Fake recommendation surfaces
- Content feed as primary home content
- Oversized decorative cards with low utility

This system keeps your product identity aligned with a **high-control, fast, offline-first download manager**.
