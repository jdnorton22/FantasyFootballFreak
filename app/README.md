# Fantasy Football Analyzer

An Android application that provides comprehensive player analysis by combining ESPN fantasy football data with NFL statistics and historical matchup performance.

## Project Structure

The application follows a clean architecture pattern with three main layers:

### Data Layer (`com.fantasyfootball.analyzer.data`)
- **local/**: Room database entities, DAOs, and local data sources
- **remote/**: Retrofit API services, DTOs, and network models  
- **repository/**: Repository implementations
- **cache/**: Cache management and utilities

### Domain Layer (`com.fantasyfootball.analyzer.domain`)
- **model/**: Domain models and entities
- **repository/**: Repository interfaces
- **usecase/**: Use cases and business logic

### Presentation Layer (`com.fantasyfootball.analyzer.presentation`)
- **ui/**: Activities, fragments, and Compose UI components
- **viewmodel/**: ViewModels and UI state management
- **navigation/**: Navigation components and utilities

## Dependencies

- **Room**: Local database and caching
- **Retrofit**: Network operations and ESPN API integration
- **Coroutines**: Asynchronous programming
- **ViewModel & LiveData**: UI state management
- **Navigation Component**: Screen navigation
- **Kotest**: Property-based testing framework
- **Hilt**: Dependency injection
- **MPAndroidChart**: Data visualization

## Requirements

- Minimum Android API level: 24
- Target SDK: 34
- Kotlin version: 1.9.10

## Testing

The project uses a dual testing approach:
- **Unit Tests**: Specific examples and integration testing
- **Property-Based Tests**: Universal properties using Kotest

## Build

To build the project:
```bash
./gradlew build
```

To run tests:
```bash
./gradlew test
```