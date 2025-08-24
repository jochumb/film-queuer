# Claude Code Configuration

## Project Structure

This project follows **Ports & Adapters (Hexagonal Architecture)** pattern with the following structure:

### Backend Structure
```
backend/src/main/kotlin/me/jochum/filmqueuer/
├── Application.kt              # Main application entry point
├── domain/                     # Core business logic (future)
│   └── (ports/interfaces will go here)
├── adapters/
│   ├── web/                    # HTTP/REST adapters (Ktor-specific)
│   │   ├── FilmController.kt   # REST endpoints
│   │   ├── FilmDto.kt          # Data transfer objects
│   │   ├── HTTP.kt             # CORS configuration
│   │   ├── Serialization.kt    # JSON serialization
│   │   └── Routing.kt          # Route configuration
│   └── persistence/            # Database adapters (future)
```

### Key Architectural Decisions

1. **Package Naming**: Uses `me.jochum.filmqueuer` as the root package
2. **Plugins as Web Adapters**: Ktor plugins (HTTP, Serialization, Routing) are treated as web adapter concerns, not separate infrastructure
3. **DTOs in Web Layer**: Data transfer objects live with their corresponding adapter
4. **Domain Layer**: Reserved for future business logic and port interfaces
5. **Hexagonal Architecture**: Clear separation between core domain and external adapters

### Technology Stack

- **Backend**: Ktor 3.2.3 + Kotlin 2.2.10
- **Frontend**: Vanilla HTML/CSS/JS with Nginx
- **Build**: Gradle with Kotlin DSL
- **Deployment**: Docker Compose

### Environment Configuration

Create a `.env` file in the project root:
```
TMDB_API_KEY=your_actual_api_key_here
```

### Development Commands

- Build: `./gradlew :backend:build`
- Run backend: `./gradlew :backend:run` (port 8080)
- Run with Docker: `docker-compose up --build`
  - Backend: http://localhost:8080
  - Frontend: http://localhost:3000

### Notes

- The domain layer is intentionally left minimal for now
- Future enhancements should respect the hexagonal architecture boundaries
- All framework-specific code should remain in adapters