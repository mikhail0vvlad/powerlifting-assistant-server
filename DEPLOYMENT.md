# Deploying to a VPS (Ubuntu 24.04)

End-to-end checklist for a fresh Ubuntu 24.04 box. Assumes you have SSH access
as a sudoer and a domain pointed at the VPS.

## 1. Install Docker

```bash
# Engine + Compose plugin from Docker's official repo
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
    https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
    | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin

# Run docker without sudo
sudo usermod -aG docker $USER
newgrp docker   # reload group in current shell
```

Verify:

```bash
docker --version
docker compose version
```

## 2. Pull the project

```bash
mkdir -p ~/apps && cd ~/apps
git clone <your-repo-url> powerlifting-assistant-server
cd powerlifting-assistant-server
```

## 3. Provide secrets

```bash
mkdir -p secrets
# Upload your firebase service-account JSON to ./secrets/firebase.json
# (e.g. via scp from your laptop)
chmod 600 secrets/firebase.json

cp .env.example .env
nano .env   # fill in DATABASE_URL etc.
```

If you use Neon as the database (recommended), `DATABASE_URL` will look like
`postgresql://user:pass@ep-xxx.eu-central-1.aws.neon.tech/db?sslmode=require`.

## 4. Build and start

```bash
docker compose up -d --build
docker compose logs -f app
```

The container exposes port `8080` on `127.0.0.1` only — don't reach for `:80`
or `:443` here, that's the reverse proxy's job (see step 5).

Smoke test from the VPS:

```bash
curl -s http://127.0.0.1:8080/health
# → {"status":"ok"}
```

## 5. Reverse proxy with TLS (Caddy)

Caddy is the simplest way to get HTTPS — it provisions Let's Encrypt certs
automatically.

```bash
sudo apt-get install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' \
    | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' \
    | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt-get update
sudo apt-get install -y caddy
```

Edit `/etc/caddy/Caddyfile`:

```caddy
api.example.com {
    encode zstd gzip
    reverse_proxy 127.0.0.1:8080 {
        # Long timeouts for slow clients on mobile
        transport http {
            response_header_timeout 60s
        }
    }
}
```

Then:

```bash
sudo systemctl reload caddy
```

Caddy will automatically obtain a Let's Encrypt certificate for the domain.
Point the Android client's `POWERLIFT_SERVER_BASE_URL` at `https://api.example.com/`.

## 6. Updating

```bash
cd ~/apps/powerlifting-assistant-server
git pull
docker compose up -d --build
docker image prune -f   # clean up old layers
```

The healthcheck means `docker compose up -d` exits only after the container
reports `healthy`.

## 7. Troubleshooting

```bash
# Container logs
docker compose logs --tail=200 app

# Container shell
docker compose exec app sh

# Restart only the app (keep DB running, if bundled)
docker compose restart app

# Full rebuild without cache (rare — use after dep changes)
docker compose build --no-cache app
```

## Resource sizing

The server is sized for a small VPS (1 vCPU / 1 GB RAM):
- `mem_limit: 768m` in compose leaves ~256 MB for the host + Caddy.
- `MaxRAMPercentage=75` lets the JVM grow up to ~575 MB inside that cap.
- Single Hikari pool of `maxPoolSize=10` is fine for one app instance.

For larger VPS, bump `mem_limit` and the Hikari `DB_POOL_SIZE` env var.
