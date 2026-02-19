# Contributing to HyperEssentials

Thank you for your interest in contributing to HyperEssentials! This document provides guidelines for contributing to the project.

## Development Setup

### Prerequisites
- Java 25 or higher
- Gradle 9.3.0 (included via wrapper)

### Building
```bash
# From the HyperSystems root directory
./gradlew :HyperEssentials:shadowJar
```

### Testing
```bash
./gradlew :HyperEssentials:test
```

## Code Style

- Use Java 25 features (records, sealed classes, pattern matching) where appropriate
- Follow existing code patterns in the codebase
- Use `@NotNull` and `@Nullable` annotations from JetBrains
- Use `ConcurrentHashMap` for thread-safe collections
- Use `CompletableFuture` for async operations
- Use `Message.join()` for formatted messages (never `.then()` or legacy codes)

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` New features
- `fix:` Bug fixes
- `docs:` Documentation changes
- `refactor:` Code refactoring
- `chore:` Build/tooling changes

## Pull Requests

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Ensure the project compiles: `./gradlew :HyperEssentials:shadowJar`
5. Submit a pull request

## License

By contributing, you agree that your contributions will be licensed under the GNU General Public License v3.0.
