# job-search

A microservices platform that helps with looking for work in IT. It serves three purposes at
once: a **practical tool** for the job hunt, a **learning vehicle** for technologies worth
getting hands-on with, and a **portfolio piece** demonstrating a full, production-style system.

> **Status: Phase 0 — foundation / walking skeleton.**
> The monorepo, build conventions, quality gates, shared libraries, and minimal local infra are
> being put in place before the first business service. The MVP pipeline (collection →
> processing → offer browsing) lands in Phase 1.

## What it will do

- **Collection** — gather job offers from the web via open APIs and scraping.
- **Processing** — normalize offers into a unified format.
- **Storage & browsing** — store and browse offers with full-text search and filtering.
- **Application tracker** — track the offers you've applied to.
- **Notifications** — alert on interesting offers based on your criteria.
- **Chatbot & CV tailoring** — AI assistance to browse offers and align your CV to a posting.

## Tech stack

- **Language / runtime:** Kotlin on Java 25 (LTS), Spring Boot.
- **Build:** Gradle (Kotlin DSL) monorepo with convention plugins and a version catalog.
- **Messaging:** Kafka for asynchronous service-to-service communication.
- **Data:** PostgreSQL (canonical store), OpenSearch (search & filtering), pgvector (AI memory/RAG).
- **APIs:** REST + OpenAPI, GraphQL, and gRPC for internal calls.
- **AI:** Spring AI for the chatbot and CV tailoring.
- **Infra & CI/CD:** AWS provisioned with Terraform, EKS + Helm, GitHub Actions.
- **Front-ends:** two SPA clients on the same backend — React + Redux and Angular + NgRx (TypeScript).

## Quality

- Spotless (ktlint) for formatting, detekt for static analysis.
- Kotest + JUnit 5, MockK, Testcontainers, WireMock for testing.
- JaCoCo coverage with an 80% build-failing gate, wired into CI.

## Running locally

```bash
# Build everything (runs tests + the 80% coverage gate)
./gradlew build

# Start local infrastructure (Kafka + schema registry; grows per phase)
docker compose up
```

## License

TBD.
