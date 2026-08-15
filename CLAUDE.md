# Claude Code Configuration

## Project Overview

**Film Queuer** is a web application for managing personalized film queues based on TMDB (The Movie Database) data, plus a **Collection** page for tracking a personal film/TV library imported from Letterboxd exports. Users can search for actors/directors, browse their filmographies, create custom watchlists with manual sorting, and import/match a Letterboxd CSV export against TMDB to track what they own and have watched.

## Project Structure

This project follows **Ports & Adapters (Hexagonal Architecture)** pattern with the following structure:

### Backend Structure
```
backend/src/main/kotlin/me/jochum/filmqueuer/
├── Application.kt              # Main application entry point
├── domain/                     # Core business logic & entities
│   ├── Person.kt               # Person entity, Department enum, defaultSortName()
│   ├── PersonRepository.kt     # Person repository interface
│   ├── PersonSelectionService.kt # Person selection business logic
│   ├── PersonEnrichmentService.kt # Backfills missing person data from TMDB (ENRICH_PERSONS)
│   ├── Queue.kt                # Queue entities (PersonQueue, NamedQueue incl. imagePath)
│   ├── QueueRepository.kt      # Queue repository interface
│   ├── QueueImageStorage.kt    # Port for downloading/persisting a local copy of a queue thumbnail
│   ├── QueueImageService.kt    # Orchestrates set/clear of a named queue's thumbnail, incl. cleanup of the old file
│   ├── Film.kt                 # Film entity (incl. directorTmdbIds, sortTitle, defaultSortTitle())
│   ├── FilmRepository.kt       # Film repository interface
│   ├── FilmEnrichmentService.kt # Backfills missing film data from TMDB (ENRICH_FILMS)
│   ├── QueueFilm.kt            # Queue-Film association entity
│   ├── QueueFilmRepository.kt  # Queue-Film repository interface
│   ├── QueueFilmService.kt     # Queue-Film business logic
│   ├── TmdbFilmFactory.kt      # Shared TMDB→Film mapping (movie/TV, director resolution, TV runtime calc)
│   ├── ExternalFilmRef.kt      # A Letterboxd-imported title, matched or not, to a Film
│   ├── ExternalFilmRefRepository.kt # ExternalFilmRef repository interface
│   ├── CollectionSortField.kt  # TITLE / YEAR / DIRECTOR / ADDED sort options for the Collection page
│   └── LetterboxdImportService.kt # CSV import, TMDB auto-match, manual link/relink
├── adapters/
│   ├── web/                    # HTTP/REST adapters (Ktor-specific)
│   │   ├── PersonController.kt # Person REST endpoints
│   │   ├── PersonDto.kt        # Person data transfer objects
│   │   ├── QueueController.kt  # Queue REST endpoints
│   │   ├── QueueDto.kt         # Queue data transfer objects
│   │   ├── QueueFilmDto.kt     # Queue-Film DTOs, FilmResponseDto, DirectorDto, ReorderFilmsDto
│   │   ├── FilmController.kt   # Film/TV search REST endpoints (+ sort-title update)
│   │   ├── FilmDto.kt          # Film data transfer objects
│   │   ├── CollectionController.kt # Collection import/list/link/remove REST endpoints
│   │   ├── CollectionDto.kt    # Collection data transfer objects
│   │   ├── ImageController.kt  # Serves locally-stored queue thumbnails from disk (/images/queue/{filename})
│   │   ├── DateExtensions.kt   # LocalDate ↔ String conversion utilities
│   │   ├── HTTP.kt             # CORS configuration
│   │   ├── Serialization.kt    # JSON serialization setup
│   │   └── Routing.kt          # Route configuration (image routes are top-level; the rest mounted under /api)
│   ├── persistence/            # Database adapters (MySQL + Exposed ORM)
│   │   ├── DatabaseConfig.kt   # Database connection & schema setup
│   │   ├── DatabasePurgeUtility.kt # Development database cleanup
│   │   ├── PersonEnrichmentUtility.kt # Runs PersonEnrichmentService on startup if enabled
│   │   ├── FilmEnrichmentUtility.kt # Runs FilmEnrichmentService on startup if enabled
│   │   ├── PersonTable.kt      # Person database table definition (incl. sort_name)
│   │   ├── MySqlPersonRepository.kt # Person repository implementation
│   │   ├── QueueTable.kt       # Queue database table definition
│   │   ├── MySqlQueueRepository.kt # Queue repository implementation
│   │   ├── FilmTable.kt        # Film database table definition (incl. sort_title)
│   │   ├── MySqlFilmRepository.kt # Film repository implementation
│   │   ├── FilmDirectorTable.kt # Film↔Person director join table (billing_order)
│   │   ├── ExternalFilmRefTable.kt # Letterboxd import rows (owned/watched/removed flags)
│   │   ├── MySqlExternalFilmRefRepository.kt # ExternalFilmRef repository implementation
│   │   ├── QueueFilmTable.kt   # Queue-Film join table definition
│   │   └── MySqlQueueFilmRepository.kt # Queue-Film repository implementation
│   ├── letterboxd/
│   │   └── LetterboxdCsvParser.kt # Parses Letterboxd "list" (owned) and "watched" CSV exports
│   ├── storage/
│   │   └── LocalQueueImageStorage.kt # Downloads a URL and writes it to a Docker-volume-mounted directory
│   └── tmdb/                   # TMDB API integration
│       ├── TmdbService.kt      # TMDB service interface
│       ├── TmdbClient.kt       # TMDB HTTP client implementation
│       └── TmdbModels.kt       # TMDB API response models + originalReleaseDate()/directorCrew()
```

### Frontend Structure
```
frontend/
├── index.html                  # Main HTML page (single #app mount point)
├── css/
│   └── style.css              # Application styles (design tokens, compact dark/light theme)
└── js/
    ├── app.js                 # Entry point: routing, state, event delegation, all page logic
    ├── render.js               # HTML template functions (home/manage/queue-detail/collection views)
    ├── api.js                 # API client
    ├── dragdrop.js             # Drag-and-drop reordering helper (queues and queue films)
    └── notifications.js       # Toast notifications and modal confirmations
```

## Core Features

### 1. Person Search & Management
- Search TMDB for actors, directors, writers
- Save selected persons to create film queues
- Department-based role translation (Acting → Actor, etc.)
- Toast notifications for success/error feedback

### 2. Filmography Browse & Filter
- Load person's complete filmography from TMDB
- Department switching with dropdown selector for persons with multiple roles
- Vote count filtering with adjustable threshold slider (% of average votes)
- Hide films with 0 votes and films below threshold
- Films ordered chronologically (oldest to newest)
- Automatic film deduplication with role/job concatenation

### 3. Film & TV Show Queue Management
- Add/remove films and TV shows from personal queues with toast notifications
- **Three-tab interface**: Filmography, Search Movies, Search TV Shows
- Search external TMDB database for movies and TV shows to add to queues
- Visual indicators show which films/shows are already queued
- Manual drag-and-drop reordering with persistent sort order
- Modal confirmations for destructive actions (film removal)
- Two-column responsive layout (queue on left, browse content on right)

### 4. Queue List Management
- Drag-and-drop reordering of queues themselves
- Persistent queue ordering with database sort_order field
- Clean interaction zones (drag handle left, click area right)
- Visual feedback during queue operations

### 5. User Experience & Interface
- **Consistent Navigation**: Unified header with tab-style navigation across all pages
- **Compact Queue Previews**: Streamlined home page cards with film counts and optimized spacing
- **Department Management**: Dynamic department switching based on person's actual TMDB credits
- **Toast Notifications**: Modern, non-blocking success/error/warning messages
- **Modal Confirmations**: Beautiful confirmation dialogs instead of browser alerts
- **Auto-dismiss**: Success messages disappear automatically, errors persist until dismissed
- **Responsive Design**: Mobile-friendly notifications and interactions
- **Visual Feedback**: Hover states, drag indicators, and smooth animations
- **Text Truncation**: Smart handling of long person names in queue previews

### 6. TV Show Support
- **Unified Film/TV Model**: Both movies and TV shows stored as "films" with `tv` boolean flag
- **TMDB Integration**: Separate API calls for movie details (`/movie/{id}`) vs TV details (`/tv/{id}`)
- **Runtime Calculation**: TV shows calculate total runtime by fetching all season/episode details and summing individual episode runtimes
- **TV Director Resolution**: Since TV has no single top-level `credits` list, directors are resolved from `aggregate_credits` (every episode's crew rolled up across the whole series) and ranked by episode count, so the most-consistent director leads a co-directed/anthology show's director list
- **Three-Tab Interface**: Filmography, Search Movies, Search TV Shows on queue edit pages
- **Seamless UX**: Users can add both movies and TV shows to the same queues

### 7. Database & Temporal Types
- **Proper temporal types**: `Instant` for timestamps, `LocalDate` for dates
- **UUID-based entity IDs** for all primary keys
- **Sort order support** for manual queue and film arrangement
- **TV flag**: `tv` boolean column distinguishes movies (false) from TV shows (true)

### 8. Letterboxd Collection Import & Tracking
- **CSV Import**: Upload a Letterboxd "list" export (owned) or "watched" export; rows are deduplicated by source/title/year and merge `owned`/`watched` flags onto an existing row rather than creating duplicates
- **Auto-Matching**: Each imported row is matched against TMDB movie search, narrowed by the row's known year (TMDB ranks by popularity, so year-narrowing prevents a low-profile film from being pushed off the results by an unrelated same-title film); ties (same title+year, similar vote counts) are left unmatched for manual review
- **Manual "Fix Match"**: A modal with a Movies/TV toggle and an editable year filter lets the user search TMDB directly and link a specific result, including TV mini-series (which the auto-matcher doesn't attempt)
- **Original Release Date**: Computed as the earliest Theatrical/Theatrical-limited date across every country TMDB has `release_dates` for (matching how Letterboxd defines a film's "Year"), instead of TMDB's editorially-chosen and sometimes US-biased top-level `release_date`
- **Director Data**: Movies resolve directors from TMDB credits; TV shows from `aggregate_credits` (see TV Show Support above). Stored in a `film_directors` join table with `billing_order` so co-directed films have a well-defined primary director
- **Editable Sort Name/Title**: Since TMDB has no `sort_name`/`sort_title` field, director names default to "Lastname, Firstname" and film titles default to the title with a leading "The"/"A"/"An" stripped — both are stored per-record and editable by clicking the name/title in the Collection table (fixes cases the naive default gets wrong, e.g. "Guillermo del Toro")
- **Soft-Delete ("Remove")**: Removing an item from the Collection page sets a `removed` flag rather than deleting the row, so re-importing the same Letterboxd export later never resurrects something the user intentionally hid. A "Show removed" toggle views/restores hidden items
- **Collection Page**: Owned/Watched/Unmatched-only filters, sortable by Title/Year/Director (chained Director→Year→Title, default view)/Recently-added, paginated with a direct page-number jump, and a per-row overflow menu (Fix match / Remove) so "Add to queue" stays the primary action

## Database Schema

```sql
-- Core entities
persons: tmdb_id (PK), name, department, image_path, sort_name
films: tmdb_id (PK), title, original_title, release_date (DATE), runtime (INT), genres (VARCHAR),
       poster_path (VARCHAR), tv (BOOLEAN DEFAULT FALSE), sort_title
queues: id (UUID, PK), type, person_tmdb_id, name, description, created_at (TIMESTAMP), sort_order (INT),
        image_path (VARCHAR, named queues only - a locally-served path like /images/queue/<file>.jpg,
        not the raw URL the user provided; see QueueImageService)

-- Association with manual ordering
queue_films: queue_id (UUID), film_tmdb_id, added_at (TIMESTAMP), sort_order (INT)
             PRIMARY KEY (queue_id, film_tmdb_id)

-- Director join table (co-directed films/shows keep TMDB crew order via billing_order)
film_directors: film_tmdb_id (FK → films), person_tmdb_id (FK → persons), billing_order (INT DEFAULT 0)
                PRIMARY KEY (film_tmdb_id, person_tmdb_id)

-- Letterboxd collection import rows
external_film_refs: id (UUID, PK), source (VARCHAR), title (VARCHAR), year (INT NULL),
                     film_tmdb_id (FK → films, NULL until matched), owned (BOOLEAN), watched (BOOLEAN),
                     created_at (TIMESTAMP), removed (BOOLEAN DEFAULT FALSE)
                     UNIQUE (source, title, year)
```

## API Endpoints

All routes below are mounted under `/api` (e.g. `GET /api/persons/search`).

### Person Management
- `GET /persons/search?q={query}` - Search TMDB persons
- `POST /persons/select` - Save person and create queue
- `GET /persons/{tmdbId}/filmography?department={dept}` - Get filmography with available departments
- `PUT /persons/{tmdbId}/department` - Update person's department
- `PUT /persons/{tmdbId}/sort-name` - Set a person's sort name (`{sortName}`)

### Film & TV Search
- `GET /films/search?q={query}&year={year}` - Search TMDB movies by title, optionally narrowed by year
- `GET /films/search/tv?q={query}&year={year}` - Search TMDB TV shows by title, optionally narrowed by year
- `PUT /films/{tmdbId}/sort-title` - Set a film's sort title (`{sortTitle}`)

### Queue Management
- `GET /queues` - List all queues with person data (ordered by sort_order)
- `GET /queues/previews` - Get compact queue previews with film counts for home page
- `GET /queues/{id}` - Get specific queue with person data
- `GET /queues/{id}/films` - Get queue films (ordered by sort_order)
- `POST /queues/{id}/films` - Add film/TV show to queue (includes `tv` boolean parameter)
- `DELETE /queues/{id}/films/{filmId}` - Remove film from queue
- `PUT /queues/{id}/films/reorder` - Reorder films within queue
- `PUT /queues/reorder` - Reorder queues themselves
- `POST /queues/named` - Create a named (non-person) queue
- `PUT /queues/{id}/image-path` - Set/clear a named queue's thumbnail (`{imagePath}`: a source URL to
  download and store a local copy of; blank/omitted clears it). 400 if the URL isn't a reachable
  supported image type
- `GET /images/queue/{filename}` - Serves a locally-stored queue thumbnail (top-level route, not under `/api`)
- `DELETE /queues/{id}` - Delete a queue

### Collection Management (Letterboxd import)
- `POST /collection/import/letterboxd/owned` - Import a Letterboxd "list" (owned) CSV export
- `POST /collection/import/letterboxd/watched` - Import a Letterboxd "watched" CSV export
- `GET /collection?owned={bool}&watched={bool}&unmatched={bool}&removed={bool}&sort={title|year|director|added}&order={asc|desc}&offset={n}&limit={n}` - Paginated, filtered, sorted collection listing enriched with matched film + director data
- `PUT /collection/{id}/link` - Manually link/relink a collection row to a TMDB film (`{tmdbId, tv}`)
- `PUT /collection/{id}/removed` - Soft-delete/restore a collection row (`{removed}`)

## Key Architectural Decisions

1. **Hexagonal Architecture**: Clear separation between domain and external adapters
2. **Repository Pattern**: Interface abstractions for all data access
3. **Service Layer**: Business logic separated from controllers
4. **DTO Conversion**: String ↔ temporal type conversion for API compatibility
5. **Comprehensive Testing**: Repository, service, and controller layers all tested
6. **Consistent Naming**: "MySql" prefix for repository implementations
7. **Additive vs. Replace Persistence**: `save()` on Film/ExternalFilmRef is insert-if-absent and never clobbers existing richer data (e.g. directors); `update()` is a full replace, used only when explicitly re-resolving/re-matching. Dedicated single-field updates (`updateSortName`, `updateSortTitle`, `setRemoved`) exist specifically so a general-purpose `update()`/re-import never accidentally reverts a manual correction
8. **Shared TMDB→Film Mapping**: `TmdbFilmFactory` is the one place that maps TMDB movie/TV details onto the domain `Film` shape (including TV runtime calculation and director resolution), used by both `QueueFilmService` and `LetterboxdImportService` so this logic isn't duplicated
9. **Local Copies of User-Provided Images**: Named queue thumbnails are downloaded and stored on disk (`LocalQueueImageStorage`, behind the `QueueImageStorage` port) rather than just persisting the source URL, so the app doesn't break if that URL later changes or disappears. `QueueImageService` orchestrates the download-then-persist-then-cleanup-old-file sequence so a replaced/cleared image never leaks an orphaned file. The storage directory is a Docker-mounted volume (`queue_images`) so copies survive container rebuilds

## Technology Stack

- **Backend**: Ktor 3.2.3 + Kotlin 2.2.10
- **Database**: MySQL 8.0 + Exposed ORM
- **Frontend**: Vanilla HTML/CSS/JS with drag-and-drop
- **Testing**: JUnit 5 + MockK + H2 in-memory database
- **Build**: Gradle with Kotlin DSL
- **Deployment**: Docker Compose

## Environment Configuration

Create a `.env` file in the project root:
```bash
TMDB_API_KEY=your_actual_api_key_here
DATABASE_URL=jdbc:mysql://localhost:3306/filmqueuer
DATABASE_USER=root  
DATABASE_PASSWORD=password

# Optional: Database purge modes for development
# PURGE_MODE=queues    # Purge queue-related data
# PURGE_MODE=persons   # Purge persons data
# PURGE_MODE=films     # Purge film-related data  
# PURGE_MODE=all       # Purge all tables

# Optional: one-time startup backfills for existing data missing runtime/genres/poster/release date
# ENRICH_PERSONS=true
# ENRICH_FILMS=true
```

## Development Commands

### Backend
- **Build**: `./gradlew :backend:build`
- **Run backend**: `./gradlew :backend:run` (port 8080)  
- **Run tests**: `./gradlew :backend:test`
- **Lint code**: `./gradlew :backend:lint`
- **Format code**: `./gradlew :backend:format`

### Frontend
- No build step — plain static HTML/CSS/JS, no bundler.
- **Run dev server (live-reload)**: `cd frontend && npm run dev` (browser-sync, port 3000)
- **Run tests**: `cd frontend && npm test`
- **Test with coverage**: `cd frontend && npm run test:ci`
- **Watch mode**: `cd frontend && npm run test:watch`

### Docker
- **Run with Docker**: `docker-compose up --build`
  - Backend: http://localhost:8080
  - Frontend: http://localhost:3000

## Code Quality & Testing

### Backend Testing
- **Linting**: ktlint enforces Kotlin coding standards (run via `:backend:build`, not `:backend:test` — always run a full `:backend:build` before considering backend work done)
- **Testing**: Comprehensive test coverage across all layers
  - Repository tests with H2 in-memory database
  - Service tests with MockK for mocking
  - Controller tests with Ktor testing framework
- **Architecture**: Hexagonal architecture with dependency injection

### Frontend Testing
- **Test Framework**: Jest with JSDOM for DOM simulation
- **Coverage**: 98 tests across 4 suites, covering everything except `app.js` (the imperative
  routing/orchestration layer, deliberately left to manual QA rather than unit tests)
  - **render.test.js**: Pure HTML-template and formatting helpers (roleLabel, yearOf, runtimeLabel,
    esc, and every `render*` function, including the Collection page and its modals)
  - **api.test.js**: Fetch contract (URL, method, headers, body) for every endpoint
  - **notifications.test.js**: Toast lifecycle and modal confirm/cancel/escape behavior
  - **dragdrop.test.js**: `enableDragReorder` — draggable wiring, dragover reordering, drop order
- **CI Integration**: Tests run automatically during the Docker build (`frontend/Dockerfile`
  test stage) — a failing test fails the build
- **Manual QA**: `app.js` orchestration, visual queue indicators, add/remove/watched flows

## Development Notes

- **Temporal Types**: Use `Instant` for timestamps, `LocalDate` for dates
- **Entity IDs**: All entities use UUID-based primary keys  
- **Sort Order**: Queue films support manual ordering via `sort_order` field
- **API Compatibility**: DTOs handle conversion between JSON strings and domain temporal types
- **Database Purging**: Use `PURGE_MODE` environment variable for development cleanup
- **Drag & Drop**: Frontend supports manual reordering with visual feedback
- **Future Enhancements**: Respect hexagonal architecture boundaries
- **Code Style**: Run `./gradlew :backend:format` before committing
- **Schema changes**: `SchemaUtils.create()` only creates missing tables — it never alters existing ones. Adding a column requires a manual, non-destructive `ALTER TABLE` against the running database in addition to updating the Exposed table object
- Module-based split in frontend.
