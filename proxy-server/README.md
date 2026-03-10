# ⚡ CacheProxy — Smart HTTP Caching Gateway

A production-grade full-stack caching proxy server built with **Java 17 + Spring Boot 3.2** backend and a **React + Vite** dashboard frontend.

---

## 🏗️ Architecture

```
Browser/App
    │
    ▼
React Dashboard (Port 3000)
    │
    ▼
Spring Boot Proxy (Port 8080)
    ├── GET /proxy/**        → Proxy + Cache layer → Origin Server
    ├── GET /cache/stats     → Live cache metrics
    ├── GET /cache/health    → Health check
    ├── POST /cache/clear    → Flush all cache
    └── PUT /cache/origin    → Update origin URL
    │
    ▼
Caffeine In-Memory Cache
    ├── Capacity: 10,000 entries
    ├── TTL: 1 hour
    └── Strategy: LRU eviction
```

---

## 🚀 Quick Start

### Option 1 — Docker (Recommended)

```bash
# Linux / macOS
chmod +x run.sh && ./run.sh

# Windows
run.bat
```

### Option 2 — Manual

**Backend:**
```bash
cd backend
mvn spring-boot:run
# Runs on http://localhost:8080
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:3000
```

---

## 🌐 Usage

### Proxy a Request

```bash
# First request — CACHE MISS (fetches from origin)
curl http://localhost:8080/proxy/products/1?origin=http://dummyjson.com

# Second request — CACHE HIT (instant!)
curl http://localhost:8080/proxy/products/1?origin=http://dummyjson.com

# Check response headers
curl -I http://localhost:8080/proxy/products/1?origin=http://dummyjson.com
# X-Cache: HIT
# X-Proxy: CachingProxy/1.0
# X-Response-Time: 0ms
```

### Cache Management

```bash
# View stats
curl http://localhost:8080/cache/stats

# Health check
curl http://localhost:8080/cache/health

# Clear all cache
curl -X POST http://localhost:8080/cache/clear

# Update origin
curl -X PUT http://localhost:8080/cache/origin \
  -H "Content-Type: application/json" \
  -d '{"origin": "https://jsonplaceholder.typicode.com"}'
```

---

## 📊 Dashboard Features

| Tab | Features |
|-----|----------|
| **Dashboard** | Hit rate gauge, live stats cards, top endpoints chart, request log |
| **Request Tester** | Send proxy requests, see HIT/MISS badges, view JSON responses |
| **Settings** | Update origin URL, clear cache |

---

## ⚙️ Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PROXY_ORIGIN` | `http://dummyjson.com` | Default target server |
| `SERVER_PORT` | `8080` | Backend port |

### Cache Settings (CacheConfig.java)

```java
.maximumSize(10_000)                    // Max cached items
.expireAfterWrite(Duration.ofHours(1)) // Cache TTL
.recordStats()                          // Enable metrics
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend Framework | Spring Boot 3.2 |
| Language | Java 17 |
| Cache Engine | Caffeine (Google) |
| HTTP Client | Spring WebFlux |
| Monitoring | Spring Actuator + Micrometer |
| Frontend | React 18 + Vite |
| Containerization | Docker + Docker Compose |

---

## 📈 Performance

| Scenario | Response Time |
|----------|--------------|
| Cache HIT | < 1ms |
| Cache MISS (local network) | 50–200ms |
| Cache MISS (external API) | 200–800ms |

**Result: 99%+ latency reduction on cache hits.**
