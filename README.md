# Film Queuer

A film queuing application with person search functionality using TMDB API.

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
./gradlew :backend:test      # Run tests
./gradlew :backend:lint      # Run linting
./gradlew :backend:format    # Format code
```

## Tech Stack
- **Backend**: Kotlin + Ktor + MySQL + Exposed ORM
- **Frontend**: Vanilla JavaScript
- **Infrastructure**: Docker Compose