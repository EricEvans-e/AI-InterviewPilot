# AGENTS.md

Quick-reference for AI coding agents working in this monorepo.

## Repo Layout

Two sibling projects, one repo root:

```
AI-Meeting/              # Java 17 Spring Boot backend (Maven)
AI-Meeting-Frontend/     # React 19 + TypeScript + Vite frontend
docs/                    # PRDs, run guides, deployment docs
src/                     # Raw question-bank resources (Excel/docx)
scripts/                 # Helper scripts
```

Backend is a modular monolith — single Maven module `admin/` under root POM `AI-Meeting/pom.xml`. Package root: `com.interviewpilot`. Domains: `interview`, `ai`, `agent`, `media`, `user`, `auth`, `common`.

## Commands That Matter

### Backend (run from `AI-Meeting/`)

```bash
# Verify build (CI skips tests too)
./mvnw -B -ntp clean verify -Dmaven.test.skip=true

# Run tests only
./mvnw -q -pl admin test

# Start dev server (port 8002)
./mvnw spring-boot:run -pl admin

# Format (Spotless — runs automatically during verify)
mvn spotless:apply
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

### Frontend (run from `AI-Meeting-Frontend/`)

```bash
npm ci
npm run dev           # Vite dev server on port 5173
npm run check         # lint + typecheck + test (the gate)
npm run build         # tsc -b && vite build
npm run test:run      # single-shot vitest (no watch)
npm run lint          # eslint only
npm run typecheck     # tsc --noEmit -p tsconfig.app.json
```

**Always run `npm run check` before claiming work is done.** It runs lint, typecheck, and tests in sequence.

### CI (what the gates actually run)

- **Backend CI**: `./mvnw -B -ntp clean verify -Dmaven.test.skip=true` — builds and checks formatting only, no unit tests in CI.
- **Frontend CI**: `npm ci && npm run check && npm run build` — lint + typecheck + test + build.

## Local Infrastructure

Start only the databases for local dev (not the full stack):

```bash
cd AI-Meeting
docker-compose up -d mysql mongo redis
```

Ports: MySQL `3307`, MongoDB `27017`, Redis `6379`.

The `.env` file at `AI-Meeting/.env` is auto-loaded by the backend. It's gitignored — never commit real keys. Required env vars: `MIMO_API_KEY` and `SPRING_AI_OPENAI_API_KEY`. See `.env.example` for all options.

## Key Gotchas

- **MySQL is on port 3307, not 3306.** The docker-compose maps 3307:3306. Connection string in `application.yaml` defaults to `127.0.0.1:3307`.
- **`docker-compose up -d` starts the backend container too.** For local dev, use `docker-compose up -d mysql mongo redis` to avoid port conflicts.
- **Mimo is the default AI provider.** Xunfei is legacy and disabled (`LEGACY_XUNFEI_ENABLED=false`). Never assume Xunfei is active.
- **Two databases exist.** MySQL `mainshi_agent` for structured data; MongoDB `interview_pilot` for sessions/messages.
- **API prefix is `/api/ip/v1/`.** All frontend-to-backend calls go through this.
- **Backend Spotless plugin runs during `verify`.** It sorts POM XML and trims trailing whitespace on YAML/gitignore/dockerignore files. If CI fails on formatting, run `mvn spotless:apply`.
- **Frontend has husky hooks.** Pre-commit runs `lint-staged` (eslint + prettier). Commit messages must follow Conventional Commits (enforced by commitlint).
- **Default admin credentials**: `admin` / `admin`.
- **Vite proxies `/api` and `/recordings`** to `http://localhost:8002`. If frontend gets 404, check backend is running on 8002.
- **`@` alias** in frontend maps to `./src` (configured in `vite.config.ts`).
- **Backend env auto-loading**: The backend loads `AI-Meeting/.env` and also walks up to the parent directory's `.env` when started from `admin/`. Shell env vars override `.env` values.

## Code Style

- **Backend**: Google Java Style. Spotless enforces formatting. Lombok is available globally.
- **Frontend**: ESLint + Prettier + TypeScript. No `.prettierrc` (uses defaults). `react-refresh/only-export-components` rule is off.
- **Commit messages**: Conventional Commits format (`feat:`, `fix:`, `refactor:`, etc.), enforced by commitlint on frontend.

## Domain Skills

`AI-Meeting/skills/` contains structured knowledge bases for each domain. Load the relevant skill when working on that area:

| Skill | When to use |
|-------|------------|
| `interview-pilot-repo-map` | Navigating the codebase, finding modules |
| `interview-pilot-interview-domain` | Interview flow, state machine, scoring |
| `interview-pilot-agent-domain` | Agent scene binding, provider config |
| `interview-pilot-ai-runtime` | AI chat handlers, Mimo integration |
| `interview-pilot-media-domain` | ASR WebSocket, TTS endpoints |
| `interview-pilot-auth-user` | Sa-Token auth, roles, permissions |
| `interview-pilot-debug-playbook` | Debugging common failures |
| `interview-pilot-change-playbook` | Making cross-domain changes safely |

## Security

- Never commit real API keys. Database stores `MIMO_API_KEY` as a placeholder; the backend resolves it at runtime from env vars.
- Scan before committing: `rg -n "tp-[A-Za-z0-9]{20,}" . -S`
- `.env`, `.env.local`, `.env.production` are all gitignored.

## Testing Notes

- Backend CI does **not** run unit tests (uses `-Dmaven.test.skip=true`). Local verification with `./mvnw test` is the real test gate.
- Frontend `npm run test:run` runs vitest in single-shot mode. The `npm run test` script enters watch mode.
- Frontend uses jsdom environment for testing.
