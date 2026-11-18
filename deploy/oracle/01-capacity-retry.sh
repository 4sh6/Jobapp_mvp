#!/usr/bin/env bash
# Retry launching an Always Free Ampere A1 instance until capacity appears.
#
# Free ARM capacity is the hard part of this whole migration: Oracle hands it
# out as other users release it, so "Out of host capacity" is the normal first
# response, not an error in your setup. People routinely retry for hours or
# days. This loops over every availability domain in your region so you are not
# betting on just one.
#
# Prereqs on your Mac:
#   brew install oci-cli          # or: brew install oracle/oci-cli/oci-cli
#   oci setup config             # creates ~/.oci/config (needs tenancy + user OCID)
#
# Fill in oci-vars.env, then:  bash 01-capacity-retry.sh
set -uo pipefail

VARS="${1:-$(dirname "$0")/oci-vars.env}"
if [ ! -f "$VARS" ]; then
  cat > "$VARS" <<'TEMPLATE'
# Values come from the OCI console.
# Compartment  : Identity > Compartments (use the root/tenancy OCID for a fresh account)
# Subnet       : Networking > VCN > Subnets (use the PUBLIC subnet)
# Image        : Compute > Instances > Create > Change image > Ubuntu (copy the
#                ARM/aarch64 image OCID for your region)
COMPARTMENT_OCID=
SUBNET_OCID=
IMAGE_OCID=
SSH_PUBKEY_PATH=$HOME/.ssh/id_ed25519.pub
DISPLAY_NAME=koderhyre
# Always Free ARM envelope as of 2026: 2 OCPU / 12 GB total.
OCPUS=2
MEMORY_GB=12
SLEEP_SECONDS=120
TEMPLATE
  echo "Created template: $VARS"
  echo "Fill it in, then re-run this script."
  exit 1
fi

# shellcheck disable=SC1090
set -a; . "$VARS"; set +a

command -v oci >/dev/null || { echo "oci CLI not installed (brew install oci-cli)" >&2; exit 1; }
for v in COMPARTMENT_OCID SUBNET_OCID IMAGE_OCID; do
  [ -n "${!v:-}" ] || { echo "$v is empty in $VARS" >&2; exit 1; }
done
[ -f "$SSH_PUBKEY_PATH" ] || { echo "no SSH public key at $SSH_PUBKEY_PATH" >&2; exit 1; }

echo "==> discovering availability domains"
# NOTE: no `mapfile` here on purpose — macOS ships bash 3.2, which does not
# have it. This read loop works on both 3.2 and 5.x.
ADS=()
while IFS= read -r line; do
  [ -n "$line" ] && ADS+=("$line")
done < <(oci iam availability-domain list \
  --compartment-id "$COMPARTMENT_OCID" --query 'data[].name' --raw-output \
  | tr -d '[]"' | tr ',' '\n' | sed 's/^ *//' | sed '/^$/d')
[ "${#ADS[@]}" -gt 0 ] || { echo "could not list availability domains" >&2; exit 1; }
printf '    %s\n' "${ADS[@]}"

attempt=0
while true; do
  for ad in "${ADS[@]}"; do
    attempt=$((attempt + 1))
    printf '[%s] attempt %d in %s ... ' "$(date +%H:%M:%S)" "$attempt" "$ad"

    out=$(oci compute instance launch \
      --availability-domain "$ad" \
      --compartment-id "$COMPARTMENT_OCID" \
      --subnet-id "$SUBNET_OCID" \
      --image-id "$IMAGE_OCID" \
      --shape VM.Standard.A1.Flex \
      --shape-config "{\"ocpus\":$OCPUS,\"memoryInGBs\":$MEMORY_GB}" \
      --display-name "$DISPLAY_NAME" \
      --assign-public-ip true \
      --ssh-authorized-keys-file "$SSH_PUBKEY_PATH" \
      --wait-for-state RUNNING \
      2>&1)
    rc=$?

    if [ $rc -eq 0 ]; then
      echo "SUCCESS"
      echo "$out" | grep -iE '"id"|"display-name"|"lifecycle-state"' | head -5
      echo
      echo "==> public IP:"
      oci compute instance list-vnics --instance-id \
        "$(echo "$out" | sed -n 's/.*"id": "\(ocid1.instance[^"]*\)".*/\1/p' | head -1)" \
        --query 'data[0]."public-ip"' --raw-output 2>/dev/null || \
        echo "    (check the console)"
      echo
      echo "Next: point koderhyre.com DNS at that IP (Cloudflare proxy OFF),"
      echo "      then scp this directory up and run 02-provision.sh."
      exit 0
    fi

    if echo "$out" | grep -qi 'out of host capacity'; then
      echo "out of capacity"
    elif echo "$out" | grep -qi 'LimitExceeded\|QuotaExceeded'; then
      echo "LIMIT/QUOTA EXCEEDED — you may already have an A1 instance, or be"
      echo "asking for more than the Always Free envelope ($OCPUS OCPU / ${MEMORY_GB}GB)."
      echo "$out" | head -5
      exit 1
    else
      echo "unexpected error:"
      echo "$out" | head -8
      echo "(not retrying — fix the above first)"
      exit 1
    fi
  done
  sleep "$SLEEP_SECONDS"
done
