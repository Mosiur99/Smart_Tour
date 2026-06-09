# SmartTour — Go Live

## Option 1: Run locally (fastest)

```bash
export JAVA_HOME=/usr/lib/jvm/java-19-openjdk-amd64
./scripts/run-live.sh
```

Open: **http://localhost:8080**

---

## Option 2: Docker (recommended for server/VPS)

```bash
docker compose up -d --build
```

Open: **http://localhost:8080**

Stop: `docker compose down`

---

## Option 3: Free public URL — Render.com

1. Push project to GitHub:
   ```bash
   git init
   git add .
   git commit -m "SmartTour live deploy"
   git remote add origin https://github.com/YOUR_USER/smart-tour-planner.git
   git push -u origin main
   ```

2. Go to [render.com](https://render.com) → **New +** → **Web Service**

3. Connect your repo → Render detects `render.yaml` automatically

4. Deploy → you get a URL like `https://smarttour.onrender.com`

---

## Option 4: VPS (Ubuntu) with systemd

```bash
# On server
sudo apt update && sudo apt install -y openjdk-19-jdk maven
git clone YOUR_REPO_URL /opt/smarttour
cd /opt/smarttour
mvn package -DskipTests
```

Create `/etc/systemd/system/smarttour.service`:

```ini
[Unit]
Description=SmartTour
After=network.target

[Service]
User=www-data
WorkingDirectory=/opt/smarttour
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=PORT=8080
ExecStart=/usr/bin/java -jar /opt/smarttour/target/smart-tour-planner-1.0.0.war
Restart=always

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable smarttour
sudo systemctl start smarttour
```

Use Nginx as reverse proxy on port 80/443 pointing to `localhost:8080`.

---

## Option 5: Temporary public link (testing)

```bash
# Cloudflare Tunnel (if installed)
cloudflared tunnel --url http://localhost:8080

# Or ngrok
ngrok http 8080
```

---

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | 8080 | Server port |
| `SPRING_PROFILES_ACTIVE` | — | Set to `prod` for live |

---

## Requirements

- Java 19+ (or use Docker)
- No database needed
- ~512 MB RAM minimum
