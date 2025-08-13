# TMDB API Integration

This package contains the implementation for integrating with The Movie Database (TMDB) API.

## Structure

- **api/**: Contains the Retrofit API interface definitions
- **dto/**: Data Transfer Objects for API responses
- **mapper/**: Mappers to convert DTOs to domain models
- **util/**: Utility classes for image URL handling

## Key Components

### TmdbApi
Retrofit interface defining all TMDB API endpoints:
- `getTrendingMovies()` - Get trending movies
- `searchMovies()` - Search for movies by query
- `getMovieDetails()` - Get detailed movie information
- `getWatchProviders()` - Get streaming provider availability
- `getMovieGenres()` - Get list of movie genres

### TmdbRepository
Implementation of `MovieRepository` interface that:
- Fetches data from TMDB API
- Maps DTOs to domain models
- Caches genre information to reduce API calls
- Handles errors and provides meaningful exceptions

### TmdbImageUtil
Utility for building TMDB image URLs:
- Supports different poster and logo sizes
- Provides optimal size selection based on screen width
- Handles null image paths gracefully

## Usage

The repository is automatically injected via Hilt. To use it:

```kotlin
@Inject
lateinit var movieRepository: MovieRepository

// Get trending movies
val movies = movieRepository.getTrendingMovies(page = 1)

// Search for movies
val searchResults = movieRepository.searchMovies("Avengers", page = 1)

// Get movie details
val movie = movieRepository.getMovieDetails(movieId = 123)

// Get streaming providers
val providers = movieRepository.getStreamingProviders(movieId = 123)
```

## Configuration

Make sure to add your TMDB API key to `local.properties`:
```
TMDB_API_KEY=your_api_key_here
```

## Testing

Unit tests are provided for all components:
- `TmdbImageUtilTest` - Tests image URL utilities
- `TmdbMapperTest` - Tests DTO to domain model mapping
- `TmdbRepositoryTest` - Tests repository functionality
- `TmdbApiTest` - Tests API interface contracts