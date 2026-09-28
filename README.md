# How to inject Spock mocks into Spring integration tests

This project is an example guide showing how to use Spock with Spring Boot, mixing a real Spring context with Spock mocks.

Full integration tests with the entire Spring context are valuable, but there are usually a few problem spots (like calls to an external service) that you would rather mock. Spock's `DetachedMockFactory` lets you create Spock mocks outside a spec. Combined with Spring Boot's `@TestConfiguration`, you can register those mocks as Spring beans and inject them into your specs.

The heart of this example is `PersonControllerIntTest`. It starts a Spring context and makes `MockMvc` calls to the REST endpoints. Those calls read data from a real database through a Spring Data repository, but the "Rank" data that would normally come from an external service is mocked.

## Tech stack

| Component    | Version          |
|--------------|------------------|
| Java         | 17 or newer (tested on 21) |
| Spring Boot  | 4.1.1            |
| Groovy       | 5.0              |
| Spock        | 2.4              |
| Gradle       | 9.3.1 (via the included wrapper, no install needed) |
| Databases    | H2 in-memory (default) or PostgreSQL |

## Prerequisites

1. **A JDK, version 17 or newer.** Check with `java -version`.
   - macOS: `brew install --cask temurin@21`
   - Windows: `winget install EclipseAdoptium.Temurin.21.JDK`, or download it from <https://adoptium.net>
2. **Git**
3. **(Optional) PostgreSQL**, if you want to use it instead of the in-memory database. Docker is the easiest way to get it, see [Option B](#option-b-postgresql) below.

You do **not** need to install Gradle. The `gradlew` (macOS/Linux) and `gradlew.bat` (Windows) wrapper scripts download the right version automatically.

## Step-by-step: run the app

### 1. Clone the project

```bash
git clone <repo-url>
cd spring-spock-integration-testing
```

### 2. Create your `.env` file

All passwords, API keys and connection settings live in a `.env` file in the project root. The file is git-ignored, so secrets are never committed. Start from the template:

macOS / Linux:
```bash
cp .env.example .env
```

Windows (PowerShell):
```powershell
Copy-Item .env.example .env
```

Windows (Command Prompt):
```cmd
copy .env.example .env
```

The app reads `.env` automatically at start-up, so there is nothing to `source` or `export`. Any real environment variable with the same name overrides the value in `.env`.

| Variable | Used for | Default |
|----------|----------|---------|
| `SPRING_PROFILES_ACTIVE` | Empty for H2, `postgres` for PostgreSQL | *(empty)* |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | H2 connection | `jdbc:h2:mem:testdb`, `sa`, *(empty)* |
| `H2_CONSOLE_ENABLED` | Turn the H2 web console on or off | `true` |
| `POSTGRES_URL`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | PostgreSQL connection | see `.env.example` |
| `RANKING_API_URL`, `RANKING_API_KEY` | Optional external ranking service | *(empty = return "Unranked")* |
| `SHOW_SQL` | Log the SQL that JPA generates | `false` |

### 3. Choose a database

#### Option A: H2 in-memory (default, nothing to install)

Leave `SPRING_PROFILES_ACTIVE` empty in `.env`. The database is created fresh on every start and seeded with one person (Capt James Kirk).

To keep the data between restarts, use a file-based H2 database instead:
```properties
DB_URL=jdbc:h2:file:./data/h2-database
```

#### Option B: PostgreSQL

1. In `.env`, set:
   ```properties
   SPRING_PROFILES_ACTIVE=postgres
   POSTGRES_PASSWORD=<pick a password>
   ```
2. Start PostgreSQL. The included `docker-compose.yml` reads the same `.env` file:
   ```bash
   docker compose up -d
   ```
   If you already have your own PostgreSQL server, create a database and user instead, and set `POSTGRES_URL`, `POSTGRES_USER` and `POSTGRES_PASSWORD` in `.env` to match.

The tables (`schema.sql`) and seed data (`data.sql`) are created automatically on start-up. Both scripts are written to run unchanged on H2 and PostgreSQL, and they are safe to run again on every restart.

### 4. Start the app

macOS / Linux:
```bash
./gradlew bootRun
```

Windows (PowerShell or Command Prompt):
```powershell
.\gradlew.bat bootRun
```

Wait until the log shows `Started Application`. The app listens on <http://localhost:8080>.

To pick the database for a single run without editing `.env`:
```bash
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

### 5. Try the endpoints

| Method & URL | Description | Example response |
|--------------|-------------|------------------|
| `GET /persons` | List all persons | `[{"id":1,"firstName":"James","lastName":"Kirk","title":"Capt"}]` |
| `GET /persons?lastName=Kir` | Filter by last-name prefix | `[{"id":1, ...}]` |
| `GET /persons/{id}` | Get one person (`404` if unknown) | `{"id":1,"firstName":"James",...}` |
| `GET /persons/{id}/rank` | Name plus rank from the external ranking service (`404` if unknown) | `Capt James Kirk ~ Unranked:Level 0` |

```bash
curl http://localhost:8080/persons
curl http://localhost:8080/persons/1/rank
```

(On Windows PowerShell, use `curl.exe` or `Invoke-RestMethod`, or open the URLs in a browser.)

When the H2 profile is active, the H2 web console is at <http://localhost:8080/h2-console>. Log in with the JDBC URL, user and password from your `.env`.

Stop the app with `Ctrl+C`.

### 6. Build a runnable jar (optional)

```bash
./gradlew bootJar          # Windows: .\gradlew.bat bootJar
java -jar build/libs/spring-spock-integration-testing-0.0.1-SNAPSHOT.jar
```

Run the jar from the project root so that it finds your `.env` file.

## Running the tests

macOS / Linux:
```bash
./gradlew test
```

Windows:
```powershell
.\gradlew.bat test
```

The HTML report is written to `build/reports/tests/test/index.html`.

Tests always use their own in-memory H2 database (`src/test/resources/application-test.properties`), no matter what your `.env` contains.

| Spec | What it covers |
|------|----------------|
| `PersonControllerIntTest` | Every REST endpoint through `MockMvc` with a full Spring context, including 404/400 cases. The external ranking service is a Spock mock injected through `IntegrationTestMockingConfig`. |
| `PersonServiceTest` | Plain unit test of `PersonService` with Spock mocks, without Spring |
| `ExternalRankingServiceTest` | The HTTP client for the ranking API, including the `X-API-KEY` header and error handling, stubbed with `MockRestServiceServer` |
| `PersonRepoTest` | The Spring Data repository against the real schema and seed data (`@DataJpaTest`) |
| `PostgresProfileIntTest` | Starts the app with the `postgres` profile against a real PostgreSQL in Docker (Testcontainers). **Skipped automatically when Docker is not running.** |
| `ApplicationIntTests` | The application context starts |

## How the mocking works

`IntegrationTestMockingConfig` is a `@TestConfiguration` that uses `DetachedMockFactory` to create a Spock mock of `ExternalRankingService` and registers it as the `@Primary` bean. `PersonControllerIntTest` imports that configuration and `@Autowired`s the mock, so each feature method can stub or verify interactions (`1 * externalRankingServiceMock.getRank(_) >> ...`) while everything else, including the controller, service, repository and database, is real.

Sharing the configuration in its own class, rather than nesting it in each spec, lets Spring cache and reuse the context across integration tests.

## Cross-platform notes

- `.gitattributes` keeps `gradlew` and source files with LF line endings and `gradlew.bat` with CRLF, so a checkout works on macOS, Linux and Windows. If `./gradlew` fails on macOS/Linux with `bad interpreter` or `$'\r': command not found`, the file was checked out with Windows line endings. Run `git add --renormalize . && git checkout -- gradlew` to fix it.
- If `./gradlew` reports `Permission denied` on macOS/Linux, run `chmod +x gradlew`.
- All paths in the configuration are relative (`./data/...`, `.env`), so there are no OS-specific paths.
- CI (`.github/workflows/build.yml`) runs the test suite on Ubuntu, macOS and Windows.
