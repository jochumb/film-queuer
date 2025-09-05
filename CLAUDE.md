# Claude Code Configuration

## Project Overview

**Film Queuer** is a web application for managing personalized film queues based on TMDB (The Movie Database) data. Users can search for actors/directors, browse their filmographies, and create custom watchlists with manual sorting.

## Project Structure

This project follows **Ports & Adapters (Hexagonal Architecture)** pattern with the following structure:

### Backend Structure
```
backend/src/main/kotlin/me/jochum/filmqueuer/
├── Application.kt              # Main application entry point
├── domain/                     # Core business logic & entities
│   ├── Person.kt               # Person entity and Department enum
│   ├── PersonRepository.kt     # Person repository interface
│   ├── PersonSelectionService.kt # Person selection business logic
│   ├── Queue.kt                # Queue entities (PersonQueue)
│   ├── QueueRepository.kt      # Queue repository interface  
│   ├── Film.kt                 # Film entity
│   ├── FilmRepository.kt       # Film repository interface
│   ├── QueueFilm.kt            # Queue-Film association entity
│   ├── QueueFilmRepository.kt  # Queue-Film repository interface
│   └── QueueFilmService.kt     # Queue-Film business logic
├── adapters/
│   ├── web/                    # HTTP/REST adapters (Ktor-specific)
│   │   ├── PersonController.kt # Person REST endpoints
│   │   ├── PersonDto.kt        # Person data transfer objects
│   │   ├── QueueController.kt  # Queue REST endpoints
│   │   ├── QueueDto.kt         # Queue data transfer objects
│   │   ├── QueueFilmDto.kt     # Queue-Film DTOs + ReorderFilmsDto
│   │   ├── FilmDto.kt          # Film data transfer objects
│   │   ├── DateExtensions.kt   # LocalDate ↔ String conversion utilities
│   │   ├── HTTP.kt             # CORS configuration
│   │   ├── Serialization.kt    # JSON serialization setup
│   │   └── Routing.kt          # Route configuration
│   ├── persistence/            # Database adapters (MySQL + Exposed ORM)
│   │   ├── DatabaseConfig.kt   # Database connection & schema setup
│   │   ├── DatabasePurgeUtility.kt # Development database cleanup
│   │   ├── PersonTable.kt      # Person database table definition
│   │   ├── MySqlPersonRepository.kt # Person repository implementation
│   │   ├── QueueTable.kt       # Queue database table definition
│   │   ├── MySqlQueueRepository.kt # Queue repository implementation
│   │   ├── FilmTable.kt        # Film database table definition
│   │   ├── MySqlFilmRepository.kt # Film repository implementation
│   │   ├── QueueFilmTable.kt   # Queue-Film join table definition
│   │   └── MySqlQueueFilmRepository.kt # Queue-Film repository implementation
│   └── tmdb/                   # TMDB API integration
│       ├── TmdbService.kt      # TMDB service interface
│       ├── TmdbClient.kt       # TMDB HTTP client implementation
│       └── TmdbModels.kt       # TMDB API response models
```

### Frontend Structure
```
frontend/
├── index.html                  # Main HTML page
├── css/
│   └── style.css              # Application styles (includes drag-and-drop + notifications)
└── js/
    ├── app.js                 # Main JavaScript entry point
    ├── api.js                 # API client with queue reordering endpoints
    ├── search.js              # Person search functionality
    ├── queue.js               # Queue management and film operations
    ├── ui.js                  # UI rendering and display logic
    ├── navigation.js          # SPA routing and navigation
    ├── dragdrop.js            # Drag-and-drop functionality for queues and films
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
- **Three-Tab Interface**: Filmography, Search Movies, Search TV Shows on queue edit pages
- **Seamless UX**: Users can add both movies and TV shows to the same queues

### 7. Database & Temporal Types
- **Proper temporal types**: `Instant` for timestamps, `LocalDate` for dates
- **UUID-based entity IDs** for all primary keys
- **Sort order support** for manual queue and film arrangement
- **TV flag**: `tv` boolean column distinguishes movies (false) from TV shows (true)

## Database Schema

```sql
-- Core entities
persons: tmdb_id (PK), name, department
films: tmdb_id (PK), title, original_title, release_date (DATE), runtime (INT), genres (VARCHAR), poster_path (VARCHAR), tv (BOOLEAN DEFAULT FALSE)
queues: id (UUID, PK), type, person_tmdb_id, created_at (TIMESTAMP)

-- Association with manual ordering
queue_films: queue_id (UUID), film_tmdb_id, added_at (TIMESTAMP), sort_order (INT)
             PRIMARY KEY (queue_id, film_tmdb_id)
```

## API Endpoints

### Person Management
- `GET /persons/search?q={query}` - Search TMDB persons
- `POST /persons/select` - Save person and create queue
- `GET /persons/{tmdbId}/filmography?department={dept}` - Get filmography with available departments
- `PUT /persons/{tmdbId}/department` - Update person's department

### Film & TV Search
- `GET /films/search?q={query}` - Search TMDB movies by title
- `GET /films/search/tv?q={query}` - Search TMDB TV shows by title

### Queue Management  
- `GET /queues` - List all queues with person data (ordered by sort_order)
- `GET /queues/previews` - Get compact queue previews with film counts for home page
- `GET /queues/{id}` - Get specific queue with person data
- `GET /queues/{id}/films` - Get queue films (ordered by sort_order)
- `POST /queues/{id}/films` - Add film/TV show to queue (includes `tv` boolean parameter)
- `DELETE /queues/{id}/films/{filmId}` - Remove film from queue  
- `PUT /queues/{id}/films/reorder` - Reorder films within queue
- `PUT /queues/reorder` - Reorder queues themselves

## Key Architectural Decisions

1. **Hexagonal Architecture**: Clear separation between domain and external adapters
2. **Repository Pattern**: Interface abstractions for all data access
3. **Service Layer**: Business logic separated from controllers
4. **DTO Conversion**: String ↔ temporal type conversion for API compatibility
5. **Comprehensive Testing**: Repository, service, and controller layers all tested
6. **Consistent Naming**: "MySql" prefix for repository implementations

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
```

## Development Commands

### Backend
- **Build**: `./gradlew :backend:build`
- **Run backend**: `./gradlew :backend:run` (port 8080)  
- **Run tests**: `./gradlew :backend:test`
- **Lint code**: `./gradlew :backend:lint`
- **Format code**: `./gradlew :backend:format`

### Frontend
- **Run tests**: `cd frontend && npm test`
- **Test with coverage**: `cd frontend && npm run test:ci`
- **Watch mode**: `cd frontend && npm run test:watch`

### Docker
- **Run with Docker**: `docker-compose up --build`
  - Backend: http://localhost:8080
  - Frontend: http://localhost:3000
  - **Note**: Frontend tests run automatically during Docker build

## Code Quality & Testing

### Backend Testing
- **Linting**: ktlint enforces Kotlin coding standards
- **Testing**: Comprehensive test coverage across all layers
  - Repository tests with H2 in-memory database
  - Service tests with MockK for mocking
  - Controller tests with Ktor testing framework
- **Architecture**: Hexagonal architecture with dependency injection

### Frontend Testing  
- **Test Framework**: Jest with JSDOM for DOM simulation
- **Coverage**: 51 tests across 4 test suites
  - **search.test.js**: Department translation, search workflow, person selection (18 tests)
  - **dragdrop.test.js**: Drag-and-drop functionality for films and queues (14 tests) 
  - **notifications.test.js**: Toast notifications and modal confirmations (13 tests)
  - **navigation.test.js**: URL parsing and module structure (6 tests)
- **Mocking**: APIs, browser storage, DOM elements, and external dependencies
- **CI Integration**: Tests run automatically in Docker builds
- **Manual QA**: Drag-and-drop functionality, visual queue indicators

## Development Notes

- **Temporal Types**: Use `Instant` for timestamps, `LocalDate` for dates
- **Entity IDs**: All entities use UUID-based primary keys  
- **Sort Order**: Queue films support manual ordering via `sort_order` field
- **API Compatibility**: DTOs handle conversion between JSON strings and domain temporal types
- **Database Purging**: Use `PURGE_MODE` environment variable for development cleanup
- **Drag & Drop**: Frontend supports manual reordering with visual feedback
- **Future Enhancements**: Respect hexagonal architecture boundaries
- **Code Style**: Run `./gradlew :backend:format` before committing
- Module-based split in frontend.