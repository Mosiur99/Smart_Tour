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

The Next.js frontend lives in `web/`. Needs Node 20.9 or newer.

```bash
cd web
npm ci
npm run dev        # http://localhost:3000/design shows every component
npm test           # component, accessibility and colour contrast tests
npm run typecheck
```

Colours live in `web/src/styles/tokens.css`. Components use token names only, so a colour changes in one place.

The frontend's API types are generated from `web/openapi.json`. A backend test fails when that file no longer matches the API. After a deliberate API change, run:

```bash
mvn test -Dtest=OpenApiContractTest -Dopenapi.update=true
cd web && npm run api:types
```
