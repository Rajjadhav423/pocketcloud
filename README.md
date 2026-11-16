# PocketCloud

A self-hosted mini PaaS (think a tiny Vercel/Heroku) built to learn how deployment platforms work end to end.
Target host: a spare ARM64 Android phone running Termux. Development happens on a laptop first.

**Stack:** Java 21 · Spring Boot 4 · Gradle · PostgreSQL · Flyway · JPA

## API

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/v1/projects` | Create project → `201` + `Location` |
| `GET` | `/api/v1/projects` | List projects |
| `GET` | `/api/v1/projects/{name}` | Get one project (`404` if missing) |
| `PUT` | `/api/v1/projects/{name}` | Update repo URL / branch |
| `DELETE` | `/api/v1/projects/{name}` | Delete project and its deployments → `204` |
| `POST` | `/api/v1/projects/{name}/deployments` | Queue a deployment → `202 Accepted` |
| `GET` | `/api/v1/projects/{name}/deployments` | Deployment history, newest first |
| `GET` | `/api/v1/deployments/{id}` | Deployment status |
| `GET` | `/api/v1/deployments/{id}/logs` | Build log (plain text) |

Errors use RFC 9457 `application/problem+json` (see `GlobalExceptionHandler`).

## How a deployment is built

```
POST /deployments ──▶ row with status QUEUED
                          │
BuildWorker (every 2s) ───┤ 1. claim: UPDATE … WHERE id = (SELECT … FOR UPDATE SKIP LOCKED) → BUILDING
                          │ 2. git clone --depth 1 → data/deployments/{id}/src
                          │ 3. detect framework (FrameworkDetector)
                          │ 4. run install/build commands, output → data/deployments/{id}/build.log
                          ▼ 5. READY (with app type, start command, artifact path) or FAILED (with error)
```

| Repo contains | Detected as | Build | Result |
|---|---|---|---|
| `index.html` only | Static site | none | serve repo root |
| `package.json` with `start` | Node.js server | `npm ci`/`npm install`, `npm run build` if present | run `npm start` |
| `package.json` with only `build` | Node.js static build | `npm ci`/`npm install`, `npm run build` | serve `dist`/`build`/`out`/`public` |

One build runs at a time (npm is RAM-hungry on a phone). Builds still `BUILDING` when the app restarts are
marked `FAILED`. Settings live under `pocketcloud.build` in `application.yml`.

## Run locally

Put `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` in a `.env` file in the project root (copy `.env.example`);
it is git-ignored and loaded by `springboot4-dotenv`. Real env vars override it. Then:

```bash
./gradlew bootRun
```

Flyway creates the tables on startup. The API listens on `http://localhost:8090`.

```bash
curl -X POST localhost:8090/api/v1/projects -H "Content-Type: application/json" \
  -d '{"name":"hello","repoUrl":"https://github.com/<you>/<static-site>.git"}'
curl -X POST localhost:8090/api/v1/projects/hello/deployments      # note the id
curl localhost:8090/api/v1/deployments/<id>                          # QUEUED → BUILDING → READY
curl localhost:8090/api/v1/deployments/<id>/logs
```

Requires `git` (and `npm` for Node projects) on the PATH.

## Tests

```bash
./gradlew test
```

No database needed. `BuildPipelineTest` runs real `git` against a throwaway local repo.

## Code layout

```
com.pocketcloud
├── project/      Project entity, repository, DTOs (records), service, controller
├── deployment/   Deployment entity, status, SKIP LOCKED queue, service, controller
├── build/        Worker, pipeline, framework detection, process runner, log endpoint
└── common/       Exceptions + global error handler
```

## Roadmap

1. ✅ Control-plane API (projects, deployments)
2. ✅ Build worker: `SKIP LOCKED` queue, `git clone`, framework detection, build, logs
3. Runtime supervisor: start each app on a free port, health-check it, restart on crash
4. Router: `<project>.domain` → app (Nginx first, then a Java gateway), zero-downtime switching and rollback
5. Phone deployment: Termux, Cloudflare Tunnel (wildcard DNS), Tailscale for admin access
6. GitHub webhooks (HMAC verified), live logs (SSE), Spring Security, Redis rate limiting, metrics, CI/CD
