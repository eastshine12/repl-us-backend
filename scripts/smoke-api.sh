#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/smoke-api.sh [--with-guest-auth] <api-base-url>
  API_BASE_URL=<api-base-url> scripts/smoke-api.sh [--with-guest-auth]

Checks:
  - /actuator/health/liveness
  - /actuator/health/readiness
  - /api/auth/guest and /api/me when --with-guest-auth is provided

Examples:
  scripts/smoke-api.sh https://api.example.com
  scripts/smoke-api.sh --with-guest-auth https://api.example.com
EOF
}

with_guest_auth=false
base_url="${API_BASE_URL:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-guest-auth)
      with_guest_auth=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    -*)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 64
      ;;
    *)
      base_url="$1"
      shift
      ;;
  esac
done

if [[ -z "$base_url" ]]; then
  usage >&2
  exit 64
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required" >&2
  exit 69
fi

base_url="${base_url%/}"

request() {
  local method="$1"
  local path="$2"
  shift 2

  curl -fsS \
    --connect-timeout 5 \
    --max-time 20 \
    -X "$method" \
    "$@" \
    "$base_url$path"
}

assert_status_up() {
  local label="$1"
  local path="$2"
  local body

  body="$(request GET "$path")"
  if [[ "$body" != *'"status":"UP"'* ]]; then
    echo "$label: expected UP, got $body" >&2
    exit 1
  fi
  echo "$label: ok"
}

extract_access_token() {
  sed -nE 's/.*"accessToken"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/p'
}

assert_status_up "liveness" "/actuator/health/liveness"
assert_status_up "readiness" "/actuator/health/readiness"

if [[ "$with_guest_auth" == "true" ]]; then
  auth_body="$(
    request POST "/api/auth/guest" \
      -H "Content-Type: application/json" \
      --data '{"displayName":"Smoke Test"}'
  )"
  access_token="$(printf '%s' "$auth_body" | extract_access_token)"
  if [[ -z "$access_token" ]]; then
    echo "guest auth: accessToken missing from response" >&2
    exit 1
  fi
  echo "guest auth: ok"

  me_body="$(
    request GET "/api/me" \
      -H "Authorization: Bearer $access_token"
  )"
  if [[ "$me_body" != *'"rooms"'* ]]; then
    echo "current user: rooms missing from response" >&2
    exit 1
  fi
  echo "current user: ok"
fi
