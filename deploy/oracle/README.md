# Hosting koderhyre on Oracle Cloud Always Free

Target: **$0/month compute, forever**, on an Always Free Ampere A1 ARM instance
(2 OCPU / 12 GB RAM / 200 GB block storage / 10 TB egress per month).

CockroachDB Cloud and Cloudflare R2 stay exactly as they are — both are already
inside their own free tiers and are not touched by this migration. Only the
compute moves off Render.

Your `Dockerfile` already works unmodified on arm64 (`eclipse-temurin:21` ships
arm64 images), so the application itself needs no code changes.

---

## Steps you must do yourself (4)

These need your identity, your card, or a decision only you can make. Everything
else in this directory is automated.

### 1. Create the Oracle Cloud account
<https://signup.oraclecloud.com>

- A **credit/debit card is required** even for Always Free. Oracle places a
  temporary verification hold (roughly $1) and reverses it — it is not a charge.
  Be aware your card already failed Render's RBI recurring mandate; a one-time
  authorization is a different thing and normally works, but if it is declined
  this is where you will find out.
- **Choose the home region carefully — it is PERMANENT and cannot be changed.**
  Always Free ARM instances run *only* in your home region.

| Home region | Latency to India | Reported ARM capacity |
|---|---|---|
| Singapore (`ap-singapore-1`) | ~40-60 ms | decent — **recommended** |
| Mumbai (`ap-mumbai-1`) | ~10-30 ms | heavily contended |
| Frankfurt (`eu-frankfurt-1`) | ~120-150 ms | best odds |

Do not pick Mumbai on latency alone. A region you cannot get an instance in is
worth nothing, and the choice is irreversible.

### 2. Open ingress for 80/443 in the OCI console
VCN → your subnet → Security List → Add Ingress Rules:

| Source CIDR | Protocol | Dest. port |
|---|---|---|
| `0.0.0.0/0` | TCP | 80 |
| `0.0.0.0/0` | TCP | 443 |

This is separate from the VM's own firewall, which `02-provision.sh` handles.
Both must allow the traffic or nothing reaches the app.

### 3. Point DNS at the VM — *before* running Caddy
In Cloudflare DNS for `koderhyre.com`, change the `A` record from Render's
`216.24.57.1` to your VM's public IP. Same for the `www` record.

**Set the Cloudflare proxy to "DNS only" (grey cloud) for the first run.**
Caddy needs a direct HTTP-01 challenge to issue the TLS certificate; with the
orange-cloud proxy on, the challenge is intercepted and certificate issuance
fails. Re-enable the proxy afterwards if you want it.

### 4. Pay the outstanding Render invoice
US$24.55, Stripe invoice `AKEBHR18-0003` (September 2026). This is a debt for
service already delivered and is owed regardless of where you host from now on.
Leaving it unpaid puts the Render account — and the dashboard access to your
production config — at risk.

---

## Steps that are automated

Run in this order.

| Script | Where | What it does |
|---|---|---|
| `01-capacity-retry.sh` | your Mac | Retries `LaunchInstance` until free ARM capacity appears |
| `02-provision.sh` | on the VM | Docker, firewall (incl. the iptables gotcha), swap, Caddy |
| `docker-compose.yml` | on the VM | Builds and runs the app behind Caddy |
| `Caddyfile` | on the VM | Automatic HTTPS for koderhyre.com |
| `env.template` | on the VM | Every env var the app needs, copy to `.env` and fill |

### Quick path once the VM exists

```bash
# from your Mac — copy the kit up
scp -r deploy/oracle ubuntu@<VM_IP>:~/

# on the VM
ssh ubuntu@<VM_IP>
cd ~/oracle
sudo bash 02-provision.sh

# clone the app (private repo — use a GitHub PAT or deploy key)
git clone https://<USERNAME>:<PAT>@github.com/4sh6/Jobapp_mvp.git ~/app
cp ~/oracle/docker-compose.yml ~/oracle/Caddyfile ~/app/
cp ~/oracle/env.template ~/app/.env
nano ~/app/.env            # fill in every value — see below

cd ~/app && docker compose up -d --build
docker compose logs -f app  # watch for "Started DemoApplication"
```

### Where the env values come from

Pull them off Render's env page **before** you decommission anything:
`dashboard.render.com/web/srv-d9l2vj0u01pc73ep668g/env` (eye icon reveals each).

`APP_BASE_URL` must be `https://koderhyre.com` — it is used for OAuth redirects
and links in transactional email, so a wrong value breaks Google sign-in.

Your Google OAuth client already lists `koderhyre.com` as its redirect host, so
no change is needed in Google Cloud Console as long as the domain stays the same.

---

## What "free" actually means here

**Genuinely $0/month** for compute, with no expiry — Oracle Always Free is not a
trial. Combined with CockroachDB Basic and R2 free tiers, total hosting cost can
be $0. That part is real.

What you take on in exchange:

- **You are the sysadmin now.** OS patching, Docker updates, restarts after
  reboot, log rotation, certificate renewal (Caddy automates this one), and
  backups are yours. Render did all of it.
- **One VM, no redundancy.** If it dies or Oracle reclaims it for a terms
  violation, the site is down until you rebuild. Render ran health checks and
  restarted crashed containers automatically.
- **Idle-reclaim risk.** Oracle reserves the right to reclaim persistently idle
  Always Free compute. A low-traffic pre-launch site is exactly the profile that
  trips this. Keeping a lightweight health check hitting the site helps.
- **A card stays on file.** Provisioning anything outside the Always Free
  envelope silently starts billing. Stay on the A1 shape and the free storage
  allowance.

## Reverting to Render

Nothing here is destructive to the Render setup. The service stays suspended but
intact. To go back: pay the invoice, pick a compute plan, point DNS back to
`216.24.57.1`, and redeploy. Keep the Render service around until Oracle has
been serving real traffic for a week.
