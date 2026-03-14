# HTTP Caching Proxy Server

A proxy server that forwards HTTP requests to any external API and caches the responses. Repeated requests are served instantly from cache instead of hitting the origin API again every time.

Built with Java 17, Spring Boot, Redis, PostgreSQL, React, Docker, and GitHub Actions.

---

## How it works

When a request comes in, the proxy checks Redis for a cached response. On a cache hit it returns immediately under 5ms. On a miss it forwards the request to the origin, stores the response in Redis with a configurable TTL, and returns it to the client. A React dashboard gives visibility into hit rates, TTL status, and per-endpoint traffic in real time.

Cache hits: under 5ms
Cache misses: 200-500ms (origin dependent)
Improvement: roughly 100x on repeated requests

---

## Running locally

### Option 1 - Docker Compose (recommended)

Starts the backend, frontend, Redis, PostgreSQL, and Nginx together.

```bash
git clone https://github.com/MadhavMusale/caching-proxy
cd caching-proxy
cp .env.example .env
# fill in your values in .env
docker compose up
```

Frontend runs at http://localhost:3000
Backend runs at http://localhost:8080

### Option 2 - Manual

**Backend**

Open the project in IntelliJ and run the Spring Boot application. It uses `application-local.properties` by default which runs H2 in-memory instead of PostgreSQL and skips Redis.

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

---

## Configuration

Three config files control the application:

`src/main/resources/application.properties` - main config covering port, database connection, Redis settings, and proxy behavior

`src/main/resources/application-local.properties` - local dev overrides, uses H2 in-memory database so you don't need PostgreSQL running locally

`.env` - production secrets including database password, Redis password, and API keys. This file is never committed to Git. A `.env.example` is provided with all required keys and placeholder values.

---

## API Reference

### Proxy

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET, POST, PUT, DELETE | `/proxy/**` | API Key | Forwards request to origin and caches response |

### Cache

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/cache/health` | None | Health check |
| GET | `/cache/stats` | API Key | Cache hit/miss statistics |
| POST | `/cache/clear` | API Key | Clears all cached responses |

### Admin

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/admin/keys` | API Key | List all API keys |
| POST | `/admin/keys` | API Key | Create a new API key |
| DELETE | `/admin/keys/{id}` | API Key | Revoke an API key |

---

## Security

API keys are hashed with SHA-256 before storage. Per-IP rate limiting is enforced at 100 requests per minute via Bucket4j. SSRF protection blocks requests to internal network addresses.

---

## CI/CD

GitHub Actions handles the full build-test-deploy pipeline on every push to main. Docker Compose manages the multi-container setup for both local development and production.

---

## Live Demo

Backend is deployed on Render and publicly accessible.

Base URL: `https://caching-proxy-975c.onrender.com`

Use the health endpoint to verify the deployment is running:

```bash
curl https://caching-proxy-975c.onrender.com/cache/health
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot, Spring Security |
| Cache | Redis |
| Database | PostgreSQL (prod), H2 (local dev) |
| Frontend | React, Vite |
| Auth | SHA-256 hashed API keys, Bucket4j rate limiting |
| DevOps | Docker, Docker Compose, GitHub Actions, Render |
