# Film Queuer

A film queuing application built on TMDB (The Movie Database) data. Search for actors and
directors, browse their filmographies, and build custom watchlists ("queues") with manual
drag-and-drop sorting — for movies and TV shows alike.

It also has a **Collection** page for importing a Letterboxd CSV export (owned and/or watched)
and matching it against TMDB, so you can browse, sort by director/year/title, and track your
personal library alongside your queues.

## Getting Started

### Prerequisites
- Docker and Docker Compose
- TMDB API key (get one at [TMDB](https://www.themoviedb.org/settings/api))

### Setup

1. Create a `.env` file in the root directory:
```env
TMDB_API_KEY=your_tmdb_api_key_here
DATABASE_USER=root
DATABASE_PASSWORD=your_password_here
```

2. Start the application:
```bash
docker-compose up --build
```

3. Access the application:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080

## Development

### Backend Commands
```bash
./gradlew :backend:build     # Build, lint, and run tests (what Docker runs)
./gradlew :backend:test      # Run tests
./gradlew :backend:lint      # Run linting
./gradlew :backend:format    # Format code
```

### Frontend Commands
```bash
cd frontend
npm run dev                  # Live-reload dev server (port 3000)
npm test                     # Run tests
npm run test:ci              # Run tests with coverage
```

## Tech Stack
- **Backend**: Kotlin + Ktor + MySQL + Exposed ORM
- **Frontend**: Vanilla JavaScript
- **Infrastructure**: Docker Compose

See [CLAUDE.md](./CLAUDE.md) for the full architecture, database schema, and API reference.