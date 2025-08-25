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
│   └── style.css              # Application styles (includes drag-and-drop)
└── js/
    └── app.js                 # Main JavaScript (includes manual sorting)
```

## Core Features

### 1. Person Search & Management
- Search TMDB for actors, directors, writers
- Save selected persons to create film queues
- Department-based role translation (Acting → Actor, etc.)

### 2. Filmography Browse & Filter
- Load person's complete filmography from TMDB
- Vote count filtering with adjustable threshold slider (% of average votes)
- Hide films with 0 votes and films below threshold
- Films ordered chronologically (oldest to newest)

### 3. Film Queue Management
- Add/remove films from personal queues
- Visual indicators show which films are already queued
- Manual drag-and-drop reordering with persistent sort order
- Two-column responsive layout (filmography + queue)

### 4. Database & Temporal Types
- **Proper temporal types**: `Instant` for timestamps, `LocalDate` for dates
- **UUID-based entity IDs** for all primary keys
- **Sort order support** for manual film arrangement
- **Comprehensive migrations** with rollback support

## Database Schema

```sql
-- Core entities
persons: tmdb_id (PK), name, department
films: tmdb_id (PK), title, original_title, release_date (DATE)
queues: id (UUID, PK), type, person_tmdb_id, created_at (TIMESTAMP)

-- Association with manual ordering
queue_films: queue_id (UUID), film_tmdb_id, added_at (TIMESTAMP), sort_order (INT)
             PRIMARY KEY (queue_id, film_tmdb_id)
```

## API Endpoints

### Person Management
- `GET /persons/search?q={query}` - Search TMDB persons
- `POST /persons/select` - Save person and create queue
- `GET /persons/{tmdbId}/filmography?department={dept}` - Get filmography

### Queue Management  
- `GET /queues` - List all queues with person data
- `GET /queues/{id}/films` - Get queue films (ordered by sort_order)
- `POST /queues/{id}/films` - Add film to queue
- `DELETE /queues/{id}/films/{filmId}` - Remove film from queue  
- `PUT /queues/{id}/films/reorder` - Manual reorder via drag-and-drop

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

- **Build**: `./gradlew :backend:build`
- **Run backend**: `./gradlew :backend:run` (port 8080)  
- **Run tests**: `./gradlew :backend:test`
- **Lint code**: `./gradlew :backend:lint`
- **Format code**: `./gradlew :backend:format`
- **Run with Docker**: `docker-compose up --build`
  - Backend: http://localhost:8080
  - Frontend: http://localhost:3000

## Database Migrations

Located in `src/main/resources/db/migration/`:
- **V2_1__improve_temporal_types_safe.sql** - Recommended production migration
- **V2_2__rollback_temporal_types.sql** - Rollback migration if needed
- **README.md** - Detailed migration documentation

## Code Quality & Testing

- **Linting**: ktlint enforces Kotlin coding standards
- **Testing**: Comprehensive test coverage across all layers
  - Repository tests with H2 in-memory database
  - Service tests with MockK for mocking
  - Controller tests with Ktor testing framework
- **Architecture**: Hexagonal architecture with dependency injection
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