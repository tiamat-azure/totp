# AGENTS.md

## What this project does

A TOTP (RFC 6238) code generator demo for OPT-NC: a Spring Boot backend computes codes
from an in-memory secret, a dependency-free static frontend displays the pairing QR code
and the live counter. Two containers (backend, frontend/nginx), no database, no auth -
purely a demonstrator, not production-ready (see README "Limites assumées").

## Commands

Everything goes through the Makefile; run `make help` for the full list.

```bash
make build   # docker compose build
make start   # docker compose up -d (front on http://localhost:8090)
make stop    # docker compose down
make status  # docker compose ps
make logs    # docker compose logs -f
make test    # mvn test (backend, via maven:3.9-eclipse-temurin-21 container)
```

## Architecture

- `backend/src/main/java/nc/opt/totp/` - Spring Boot 3.3 / Java 21:
  - `TotpService` - RFC 6238/4226 code computation.
  - `Base32` - RFC 4648 encode/decode.
  - `TotpConfigStore` - current config, in-memory, lost on restart.
  - `QrCodeService` - otpauth QR code (ZXing) with embedded logo.
  - `TotpController` - REST API, documented in README.md.
- `frontend/src/` - plain HTML/CSS/JS, no build step, no dependency:
  - `app.js` drives both "pages" (pairing QR / live code), polls `/api/totp`, corrects
    clock drift against `serverTime`/`validUntil`.
  - Served by nginx (`frontend/nginx.conf`), which reverse-proxies `/api/` to
    `backend:8081` on the Docker network - same origin, no CORS needed.
- `docker-compose.yml` - backend is not published on the host, only nginx is (`8090:80`);
  backend is reached only via the internal Docker network.

## Code conventions

- Backend: standard Spring Boot layering, no framework beyond `spring-boot-starter-web`
  - ZXing. Keep the secret/config server-side and in-memory - do not add persistence
    without discussing it first (this is a deliberate demo limitation).
- Frontend: no build tooling, no framework, no new dependency - keep it that way.

## Tests

- `backend/src/test/java/...` covers the official RFC 6238 (SHA1/SHA256/SHA512) and RFC
  4648 test vectors. Run with `make test`. Any change to `TotpService` or `Base32` must
  keep these vectors passing.

## Known pitfalls

- Host port 8080 is often already taken by an unrelated local container (`infisical`); the
  frontend uses **8090** for that reason - don't revert to 8080.
- `GET /api/config` returns the secret in clear text (needed for the QR hover tooltip) -
  this is intentional for the demo, not an oversight.
- Google Authenticator ignores SHA256/SHA512 and silently falls back to SHA1.

## Configuration

- No env vars. Runtime config (secret, algorithm, digits, period) is set via
  `PUT /api/config` or `POST /api/secret/random`, held in memory only.
- Optional: the frontend can be published on the Tailscale tailnet with
  `tailscale serve --bg 8090` (see README "Accès via Tailscale"). Tailnet-only, no public
  exposure.
