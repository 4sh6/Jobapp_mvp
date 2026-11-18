#!/usr/bin/env bash
# Provision a fresh Oracle Cloud Ubuntu ARM instance to run koderhyre.
# Run ON THE VM as root:  sudo bash 02-provision.sh
#
# Idempotent — safe to re-run.
set -euo pipefail

[ "$(id -u)" -eq 0 ] || { echo "run with sudo" >&2; exit 1; }

TARGET_USER="${SUDO_USER:-ubuntu}"
echo "==> provisioning for user: $TARGET_USER"

# ---------------------------------------------------------------------------
# 1. Base packages
# ---------------------------------------------------------------------------
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y ca-certificates curl gnupg git ufw iptables-persistent

# ---------------------------------------------------------------------------
# 2. Firewall
#
# Oracle's Ubuntu images ship an iptables INPUT chain that REJECTs everything
# except SSH. This is the single most common reason a working container is
# unreachable from the internet. Opening the port in the OCI console Security
# List is necessary but NOT sufficient — the host firewall must allow it too.
# ---------------------------------------------------------------------------
echo "==> opening 80/443 in iptables"
for port in 80 443; do
  if ! iptables -C INPUT -p tcp --dport "$port" -j ACCEPT 2>/dev/null; then
    # Insert ABOVE the catch-all REJECT rather than appending after it.
    iptables -I INPUT 1 -m state --state NEW -p tcp --dport "$port" -j ACCEPT
    echo "    opened $port"
  else
    echo "    $port already open"
  fi
done
netfilter-persistent save

# ---------------------------------------------------------------------------
# 3. Swap
#
# 12 GB of RAM is ample, but a JVM under memory pressure dies badly and these
# instances ship with no swap at all. 2 GB is cheap insurance against an OOM
# kill taking the site down.
# ---------------------------------------------------------------------------
if ! swapon --show | grep -q /swapfile; then
  echo "==> creating 2G swapfile"
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
else
  echo "==> swap already present"
fi

# ---------------------------------------------------------------------------
# 4. Docker (official repo — arm64)
# ---------------------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  echo "==> installing Docker"
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io \
                     docker-buildx-plugin docker-compose-plugin
else
  echo "==> Docker already installed"
fi

usermod -aG docker "$TARGET_USER" || true
systemctl enable --now docker

# Restart containers automatically after a reboot.
# (compose uses restart: unless-stopped, which needs the daemon enabled above.)

# ---------------------------------------------------------------------------
# 5. Log rotation — a chatty Spring Boot app will fill 200 GB eventually
# ---------------------------------------------------------------------------
cat > /etc/docker/daemon.json <<'JSON'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "20m", "max-file": "5" }
}
JSON
systemctl restart docker

echo
echo "==> provisioning done"
echo
echo "Verify before deploying:"
echo "  docker --version && docker compose version"
echo "  sudo iptables -L INPUT -n --line-numbers | head"
echo "  free -h"
echo
echo "NOTE: '$TARGET_USER' was added to the docker group — log out and back in"
echo "      (or run 'newgrp docker') before running docker without sudo."
echo
echo "Next: confirm DNS for koderhyre.com points at this VM's public IP with the"
echo "      Cloudflare proxy set to DNS-only, THEN run 'docker compose up -d --build'."
echo "      Caddy cannot issue a certificate until DNS resolves here."
