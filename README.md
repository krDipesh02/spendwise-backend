# Spendwise Backend

Spring Boot API for Spendwise, backed by PostgreSQL.

## Run with Docker

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`. Configure Google OAuth credentials in `docker-compose.yml` before using Google login.

## Run locally

Requires Java and Maven. Configure the database and authentication settings in `src/main/resources/application.properties` or through environment variables, then run:

```bash
mvn spring-boot:run
```
