# شو فيني آكل بهيك سعر؟ — Backend

Spring Boot 3.5 · Java 21 · PostgreSQL 16 · Flyway · JWT · Docker

Budget-first food discovery and ordering: the customer enters a budget, the backend returns every offer that
fits and ranks it by a transparent **Best Value score**, then handles orders, payments (COD / Sham Cash),
favorites, price alerts, reviews and notifications. A role-based dashboard API serves restaurant owners and admins.

## Run it

```bash
docker compose up --build
```

| What | URL |
|---|---|
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

The compose file loads **demo data** (6 restaurants, 14 offers from the app's mock content) and sets a **dev-only
OTP `123456`** that works for every phone, because no SMS gateway is wired yet. Admin demo login: `+963900000001`.

Without Docker: `mvn spring-boot:run` (needs Java 21 and a Postgres matching `application.yml` / env vars).
Tests: `mvn test`.

### Quick walkthrough
```bash
# 1. login (OTP 123456 in dev)
curl -s localhost:8080/api/v1/auth/request-otp -H 'Content-Type: application/json' -d '{"phone":"0968726559"}'
TOKEN=$(curl -s localhost:8080/api/v1/auth/verify-otp -H 'Content-Type: application/json' \
  -d '{"phone":"0968726559","code":"123456"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

# 2. "what can I eat for 50,000?"
curl -s localhost:8080/api/v1/discover -H 'Content-Type: application/json' \
  -d '{"budget":50000,"category":"all","latitude":33.5138,"longitude":36.2765,"sort":"best_value","page":1,"limit":20}'

# 3. my profile
curl -s localhost:8080/api/v1/me -H "Authorization: Bearer $TOKEN"
```

## API map (all under `/api/v1`, JSON is snake_case)

| Area | Endpoints | Auth |
|---|---|---|
| Auth | `POST /auth/request-otp` · `/auth/verify-otp` · `/auth/refresh` · `/auth/logout` | public |
| Discovery | `POST /discover` · `GET /discover/filters` · `GET /home` · `GET /config` | public* |
| Offers | `GET /offers?filter=today\|limited` · `GET /offers/{id}` · `/offers/{id}/price-history` · `/offers/{id}/reviews` | public* |
| Restaurants | `GET /restaurants/nearby` · `/restaurants/{id}` · `/restaurants/{id}/reviews` | public* |
| Profile | `GET/PUT/DELETE /me` · `/addresses` (CRUD + `/{id}/primary`) | user |
| Orders | `POST /orders` · `GET /orders?tab=active\|completed\|cancelled` · `GET /orders/{id}` · `POST /orders/{id}/cancel` · `POST /orders/{id}/review` | user |
| Favorites | `GET/POST /favorites` · `DELETE /favorites/{id}` | user |
| Price alerts | `GET/POST /price-alerts` · `DELETE /price-alerts/{id}` | user |
| Notifications | `GET /notifications` · `/unread-count` · `POST /{id}/read` · `/read-all` | user |
| Dashboard | `/admin/restaurants`, `/menu-items`, `/delivery-zones`, `/offers`, `/orders` (+status, +payment), `/categories`, `/reviews`, `/users`, `/stats/overview`, `/uploads` | owner / admin |

\* public = no token needed; a valid token additionally personalises results (`favorite` flags).
Pagination: `page` (1-based) & `limit` → `{items, page, limit, total, total_pages, has_next}`.
Errors: `{timestamp, status, code, message, path, details?, extra?}` — `message` is Arabic, `code` is stable for client-side mapping.
Language: send `Accept-Language: ar|en|fr` for categories, sort labels, order statuses, value explanations.

## How the Best Value score works (`ValueScoreCalculator`)

`value_score` (0–100) is a weighted blend of six 0..1 components. Weights live in `app.discover.weights`:

| Component | Weight | Idea |
|---|---|---|
| discount | 0.25 | % off, saturates at 50 % |
| rating | 0.25 | offer rating blended (Bayesian) with its restaurant's rating |
| portion | 0.20 | food items per 10 K SYP vs. the **category median** (inflation-proof) |
| delivery | 0.10 | fee relative to price + ETA |
| distance | 0.10 | dropped when the user shares no location |
| budget_fit | 0.10 | prefers offers that actually use the budget; dropped when no budget |

Missing components are removed and the rest re-normalised. The brief's example (A: 45 K, 4.6★, 25 % off, full meal vs
B: 48 K, 4.1★, 5 %, less food) scores **74 vs 51**. Sponsored offers are flagged (`sponsored`) but never boosted.
`GET /offers/{id}` returns `value_reasons` and `value_breakdown` for the "why is this a good deal" card.

## Decisions & assumptions worth knowing

* **Money** is whole Syrian pounds (`BIGINT`) — no decimals, no float errors.
* **IDs are UUID strings** (the brief's `"offer_123"` was illustrative).
* **The APK has no network layer yet** (no endpoint URLs or JSON keys inside it), so the contract follows the project
  document and the screens/strings found in the APK (statuses, Sham Cash transaction number, address fields,
  "choose 2 extras", notification types…). Expect small field-name tweaks when the Flutter models are connected.
* **Login**: Syrian numbers `09xxxxxxxx` / `+9639xxxxxxxx`, normalised to E.164. OTPs are hashed, expire in 5 min,
  max 5 attempts, 60 s resend cooldown, 6/hour per phone. Access token 30 min + rotating refresh token (30 d).
* **Roles**: `CUSTOMER`, `RESTAURANT_OWNER` (only own restaurants), `ADMIN`. `BOOTSTRAP_ADMIN_PHONE` becomes admin on first login;
  admins assign owners via `owner_phone` on a restaurant.
* **Orders** are single-restaurant. Server-side: price snapshot, add-on rules, stock reservation (atomic, released on cancel),
  delivery fee from the restaurant's distance-tier zones, minimum order, idempotency via `client_request_id`,
  status history timeline. Customers can cancel while `PENDING`/`CONFIRMED`.
* **Sham Cash**: the app sends `payment_reference` (transfer number); payment becomes `AWAITING_VERIFICATION` and staff confirm it with
  `PATCH /admin/orders/{id}/payment`. No gateway API is called.
* **Time rules** (open hours, weekly offer windows) use `Asia/Damascus`; timestamps are stored/returned in UTC.
* Notifications are stored in Arabic; labels/categories are trilingual. Restaurant-authored content (names, meals) is single-language.

## Before production

1. **SMS gateway** – implement `SmsSender` (`@Component @Primary`) for your provider and leave `OTP_DEV_FIXED_CODE` empty.
2. Set strong `JWT_SECRET`, `DB_PASSWORD`, `OTP_PEPPER`; set `FLYWAY_LOCATIONS=classpath:db/migration` (no demo data), `SWAGGER_ENABLED=false`, real `CORS_ALLOWED_ORIGINS` and `APP_PUBLIC_BASE_URL`.
3. Put the API behind HTTPS (reverse proxy); `X-Forwarded-*` headers are honoured.
4. Push notifications (FCM/APNs) – hook into `NotificationService.create`.
5. If you run several replicas: add ShedLock to `OfferExpiryJob`, move uploads to object storage/CDN, add rate limiting at the gateway.
6. Discovery ranks in memory after a filtered DB query – fine for a city-scale catalogue (thousands of offers);
   move scoring into SQL/search if it grows far beyond that.
7. Add integration tests (Testcontainers) for your own flows; unit tests for scoring/phone/geo/money are included.

## Layout
```
src/main/java/com/shofiniakol
  auth/ user/ security/ config/ common/      identity, JWT, shared infrastructure
  catalog/ discover/                         restaurants, offers, ranking engine, public catalog API
  order/ review/ favorite/ alert/ notification/ media/ support/
  admin/                                     restaurant-owner & admin dashboard API
src/main/resources/db/migration              V1 schema, V2 reference data (Flyway)
src/main/resources/db/demo                   V100 demo data (opt-in)
```
