#!/usr/bin/env bash
# Check the CockroachDB Cloud cluster using the credentials in deploy/oracle/.env.
#
# The password is read from the file into PGPASSWORD and never printed or passed
# on the command line (where it would show up in `ps` and shell history).
#
# Usage: bash deploy/oracle/check-cockroach.sh [path/to/.env]
set -uo pipefail

ENVFILE="${1:-$(dirname "$0")/.env}"
PSQL=/opt/homebrew/opt/libpq/bin/psql

[ -f "$ENVFILE" ] || { echo "no env file at $ENVFILE" >&2; exit 1; }
[ -x "$PSQL" ] || { echo "psql not found at $PSQL (brew install libpq)" >&2; exit 1; }

# Read without echoing. Values may contain '=' so split on the first one only.
get() { grep -E "^$1=" "$ENVFILE" | head -1 | cut -d= -f2-; }

JDBC=$(get DB_URL)
DBUSER=$(get DB_USERNAME)
export PGPASSWORD="$(get DB_PASSWORD)"

[ -n "$JDBC" ] && [ -n "$DBUSER" ] && [ -n "$PGPASSWORD" ] || {
  echo "DB_URL / DB_USERNAME / DB_PASSWORD missing from $ENVFILE" >&2; exit 1; }

# jdbc:postgresql://host:26257/db?params  ->  postgresql://host:26257/db?params
CONN="${JDBC#jdbc:}"

# sslmode=verify-full needs the CA on disk. The repo ships it.
CERT="$HOME/.postgresql/root.crt"
REPO_CERT="$(dirname "$0")/../../docker/cockroachdb-root.crt"
if [ ! -f "$CERT" ] && [ -f "$REPO_CERT" ]; then
  echo "==> installing CA cert to $CERT"
  mkdir -p "$(dirname "$CERT")"
  cp "$REPO_CERT" "$CERT"
fi

run() { "$PSQL" "$CONN" -U "$DBUSER" -X -v ON_ERROR_STOP=0 -c "$1" 2>&1 | head -"${2:-12}"; }

echo "=== 1. connectivity / version ==="
run "select version();" 4

echo
echo "=== 2. identity, database, server clock ==="
run "select current_database() as db, current_user as usr, now() as server_time;" 6

echo
echo "=== 3. row counts (is the data intact?) ==="
run "select 'jobseekers' t, count(*) n from jobseekers
union all select 'jobseeker_profiles', count(*) from jobseeker_profiles
union all select 'jobseeker_resumes',  count(*) from jobseeker_resumes
union all select 'recruiters',         count(*) from recruiters
union all select 'jobs',               count(*) from jobs
union all select 'applications',       count(*) from applications
union all select 'job_invites',        count(*) from job_invites
union all select 'referrals',          count(*) from referrals
union all select 'companies',          count(*) from companies
order by 1;" 18

echo
echo "=== 4. last write seen (newest jobseeker) ==="
run "select max(created_at) as last_signup from jobseekers;" 6

unset PGPASSWORD
echo
echo "=== done ==="
