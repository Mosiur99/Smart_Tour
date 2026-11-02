# SmartTour

**Plan Smart, Travel More**

SmartTour is a web application for planning district tours across Bangladesh. Pick a destination, choose transport and hotels, select tourist spots, and get an automatic budget breakdown — then export your plan as a PDF.

Tour data lives in PostgreSQL. The JSON files in `src/main/resources/data` are only used by the one-time import migrations.

**Live demo:** [https://smart-tour-700j.onrender.com](https://smart-tour-700j.onrender.com)

<p align="center">
  <img src="src/main/webapp/images/logo.png" alt="SmartTour logo" width="120">
</p>

---

## Features

- **64 districts** — browse by division or search from the home page
- **District guides** — tourist spots, historical places, universities, food spots, and transport options
- **Tour builder** — set origin, travelers, nights, transport, hotel, and spots in one flow
- **Budget calculator** — line-item breakdown for transport, hotel, local travel, and food
- **PDF export** — download a printable tour plan similar to a manual itinerary
- **Session-based plans** — your current plan is stored in the browser session (no login needed)

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 19, Spring Boot 3.2 |
| Views | JSP, JSTL |
| UI | Bootstrap 5, Bootstrap Icons, custom CSS |
| Data | Jackson (JSON), no database |
| PDF | OpenPDF |
| Deploy | Docker, Render.com |

---

## Prerequisites

- **Java 19** (OpenJDK or Eclipse Temurin)
- **Maven 3.8+**
- **Python 3** (optional — only for regenerating district data)

---

## Quick Start

```bash
git clone https://github.com/YOUR_USER/smart-tour-planner.git
cd smart-tour-planner

export JAVA_HOME=/usr/lib/jvm/java-19-openjdk-amd64   # adjust for your system
./scripts/run-live.sh
```

Open [http://localhost:8080](http://localhost:8080)

### Manual build & run

```bash
mvn clean package -DskipTests
java -jar target/smart-tour-planner-1.0.0.war --spring.profiles.active=prod --server.port=8080
```

### Docker

```bash
docker compose up -d --build
```

Open [http://localhost:8080](http://localhost:8080)

---

## How It Works

1. **Home** — filter districts by division and open a destination
2. **District detail** — review spots, hotels, transport, and local tips
3. **Tour builder** — configure your trip and save the plan
4. **Plan summary** — view the budget breakdown and export PDF

Budget formula:

```
Transport  = (outbound + return fare) × travelers
Hotel      = nightly rate × nights
Local      = local transport default × travelers
Food       = food default × travelers
```

Transport and hotel costs use `0` when a price is marked TBD in the data.

---

## Project Structure

```
smart-tour-planner/
├── src/main/java/com/smarttourplanner/
│   ├── controller/          # Home, District, TourPlan, Health
│   ├── service/             # Data, budget, PDF, session
│   ├── model/               # District, TourPlan, BudgetBreakdown, …
│   └── config/              # Static resources, web config
├── src/main/webapp/
│   ├── WEB-INF/views/       # JSP pages
│   ├── css/                 # SmartTour theme
│   └── images/              # Logo and assets
├── src/main/resources/data/
│   ├── districts-index.json
│   └── districts/           # One JSON file per district (64 total)
├── scripts/
│   ├── run-live.sh
│   └── sync-districts-from-dataset.py
├── Dockerfile
├── docker-compose.yml
├── render.yaml              # Render.com blueprint
└── DEPLOY.md                # Full deployment guide
```

---

## Updating District Data

District files are generated from the master dataset at the project root:

`bangladesh_64_district_tourism_dataset_v4_with_transport_costs.json`

Regenerate all district JSON files:

```bash
python3 scripts/sync-districts-from-dataset.py
```

Restart the app after syncing. See [DATA.md](DATA.md) for details.

---

## Deployment

SmartTour ships with Docker and a Render blueprint for free hosting.

| Method | Command / Link |
|--------|----------------|
| Local script | `./scripts/run-live.sh` |
| Docker | `docker compose up -d --build` |
| Render.com | Connect repo — uses `render.yaml` |
| VPS | See [DEPLOY.md](DEPLOY.md) |

Health check endpoint: `GET /health`

---

## Configuration

| Profile | File | Notes |
|---------|------|-------|
| Default | `application.properties` | Dev port `8081` |
| Production | `application-prod.properties` | Uses `PORT` env var (Render/Docker) |

Set the active profile:

```bash
export SPRING_PROFILES_ACTIVE=prod
```

---

## API Routes

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Home — district list |
| GET | `/district/{id}` | District detail |
| GET | `/build/{districtId}` | Tour builder |
| POST | `/save` | Save tour plan to session |
| GET | `/summary` | Plan summary & budget |
| GET | `/export/pdf` | Download PDF |
| GET | `/new` | Clear session and start over |
| GET | `/health` | Health check |


---

## Acknowledgements

- Bangladesh district tourism dataset (v4 with transport costs)
- Bootstrap & Bootstrap Icons
- Spring Boot & OpenPDF communities

## Run locally without Docker

Start a throwaway PostgreSQL in one terminal:

```bash
mvn test-compile org.codehaus.mojo:exec-maven-plugin:java -Dexec.classpathScope=test -Dexec.mainClass=com.smarttourplanner.support.LocalPostgres
```

Start the app in another:

```bash
DATABASE_URL=jdbc:postgresql://localhost:54329/postgres DB_USERNAME=postgres mvn spring-boot:run
```

## Speed check

```bash
mvn test -Dtest=ApiSpeedCheck -DexcludedGroups=none
```

## Web app

The Next.js frontend lives in `web/`. Needs Node 20.9 or newer. Bangla is served at `/` and English at `/en`.

| Page | Address |
|---|---|
| Plan form | `/` |
| Saved plan, shareable and printable | `/plan/<id>` |
| Destinations | `/destinations`, `/destinations/<slug>` |
| Route guide | `/routes/dhaka-to-sylhet` |
| Every component | `/design` |
| Admin panel | `/admin` (see [Admin panel](#admin-panel)) |

```bash
cd web
npm ci
npm run dev        # expects the API on http://localhost:8081 (set API_ORIGIN to change it)
npm test           # component, accessibility and colour contrast tests
npm run typecheck
```

The browser calls `/api/...` on the site itself. Next.js forwards those calls to the API, so locally every visitor reaches the API from one address and shares one rate limit. In production the CDN sends `/api` straight to the API.

Colours live in `web/src/styles/tokens.css`. Components use token names only, so a colour changes in one place.

### End-to-end tests

Nothing in the catalog is verified yet, so no real plan can be made. `web/e2e/test-data.sql` publishes Sylhet with invented places and prices for testing. Never run it on a production database.

```bash
# with the API running against a local database (see "Run locally without Docker")
psql "$DATABASE_URL" -f web/e2e/test-data.sql
cd web && npm run build && npx playwright test   # PW_CHANNEL=chrome uses the installed Chrome
```

GitHub Actions runs the same journeys, then Lighthouse with a 150KB script budget.

### API types

The frontend's API types are generated from `web/openapi.json`. A backend test fails when that file no longer matches the API. Mark a record field `@Nullable` (JSpecify) when it can be null, and the contract and types follow. After a deliberate API change, run:

```bash
mvn test -Dtest=OpenApiContractTest -Dopenapi.update=true
cd web && npm run api:types
```

## Admin panel

The admin panel is at `/admin` in the web app. Editors and publishers use it to fix data, check prices and publish them. A change to anything visitors see shows on the site within seconds, with no deploy.

| Who | Can |
|---|---|
| Editor | Add records and edit drafts |
| Publisher | Everything, including editing what visitors see and publishing, unpublishing or archiving |

How it works:

- **Login.** Cloudflare Access signs people in. The API checks Access's signed token on every request, so a request that reaches the server another way can't claim to be an admin. No passwords are stored.
- **Roles.** Admins are listed by email in configuration. There is no user table.
- **Publishing.** New records start as drafts. The database refuses to publish a record that's missing a source, checked date or other required detail, and the panel says what's missing.
- **Refreshing.** A change to a visible record raises the catalog version. This API server's cache refreshes at once and other servers' within 5 seconds. Then the API calls the website's `/hooks/catalog-changed`, which rebuilds the pages on their next visit.
- **Safety.** Every change goes to `audit_log` with the old and new values. A save from an out-of-date screen is refused instead of undoing someone else's change. Changes sent from other sites are refused.
- **Checking.** Published details last checked more than 90 days ago show at the top of the panel.

| Setting | Example | Purpose |
|---|---|---|
| `smarttour.admin.access.team-domain` | `smarttour.cloudflareaccess.com` | Turns on the Access login check |
| `smarttour.admin.access.audience` | the Access app's audience tag | Accepts only tokens for this app |
| `smarttour.admin.publishers` | `a@example.com,b@example.com` | Publisher emails |
| `smarttour.admin.editors` | `c@example.com` | Editor emails |
| `smarttour.web.refresh-url` | `http://web:3000/hooks/catalog-changed` | Where to tell the website about a change |
| `smarttour.web.refresh-secret` | a long random string | Shared with the website's `REFRESH_SECRET` |

On the website, `REFRESH_SECRET` turns on the refresh hook, and `ADMIN_HOST` limits `/admin` to the admin address.

To try it locally, run the API with a stand-in login. The app refuses to start with this setting in production or alongside Access:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--smarttour.admin.local-user=me@example.com --smarttour.admin.publishers=me@example.com"
```

## Route data

`src/main/resources/data/routes/` holds researched data for the first five routes: Cox's Bazar, Saint Martin's Island, Sylhet, Bandarban and Sajek Valley. Migration V7 loads it once. New records start as drafts, and nothing gets a checked date, so no record can be published until a person has checked it in the admin panel. Records already published are never changed.

Each price links to where it was found. Most hotel prices come from Parjatan's official site. Fares that couldn't be confirmed are left empty, so the planner lists them as "not included" instead of guessing.

`RouteDataTest` checks every record in the files: Bangla names, map positions near the destination, price ranges and their sources, and a way from Dhaka and back. `RouteDataApiTest` loads the data, publishes everything complete, and checks that each route makes a plan.

After this first load, the admin panel is where data changes.
