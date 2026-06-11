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
  - /actuator/info
  - /api/auth/guest and /api/me when --with-guest-auth is provided

Examples:
  scripts/smoke-api.sh https://api.example.com
  scripts/smoke-api.sh --with-guest-auth https://api.example.com

Environment:
  SMOKE_CONNECT_TIMEOUT_SECONDS  Curl connect timeout. Defaults to 5.
  SMOKE_CURL_TIMEOUT_SECONDS     Per-request curl timeout. Defaults to 20.
  SMOKE_RETRY_ATTEMPTS           Attempts per request. Defaults to 1.
  SMOKE_RETRY_DELAY_SECONDS      Delay between retries. Defaults to 2.
EOF
}

with_guest_auth=false
base_url="${API_BASE_URL:-}"
smoke_connect_timeout_seconds="${SMOKE_CONNECT_TIMEOUT_SECONDS:-5}"
smoke_curl_timeout_seconds="${SMOKE_CURL_TIMEOUT_SECONDS:-20}"
smoke_retry_attempts="${SMOKE_RETRY_ATTEMPTS:-1}"
smoke_retry_delay_seconds="${SMOKE_RETRY_DELAY_SECONDS:-2}"

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

require_integer_at_least() {
  local name="$1"
  local value="$2"
  local minimum="$3"

  if ! [[ "$value" =~ ^[0-9]+$ ]] || (( value < minimum )); then
    echo "$name must be an integer greater than or equal to $minimum" >&2
    exit 64
  fi
}

require_integer_at_least "SMOKE_CONNECT_TIMEOUT_SECONDS" "$smoke_connect_timeout_seconds" 1
require_integer_at_least "SMOKE_CURL_TIMEOUT_SECONDS" "$smoke_curl_timeout_seconds" 1
require_integer_at_least "SMOKE_RETRY_ATTEMPTS" "$smoke_retry_attempts" 1
require_integer_at_least "SMOKE_RETRY_DELAY_SECONDS" "$smoke_retry_delay_seconds" 0

base_url="${base_url%/}"

request() {
  local method="$1"
  local path="$2"
  shift 2

  local attempt=1
  local output
  local exit_code

  while true; do
    if output="$(
      curl -fsS \
        --connect-timeout "$smoke_connect_timeout_seconds" \
        --max-time "$smoke_curl_timeout_seconds" \
        -X "$method" \
        "$@" \
        "$base_url$path" \
        2>&1
    )"; then
      printf '%s' "$output"
      return 0
    fi

    exit_code=$?
    if (( attempt >= smoke_retry_attempts )); then
      printf '%s\n' "$output" >&2
      return "$exit_code"
    fi

    printf 'retrying %s %s after curl failure (%s/%s)\n' \
      "$method" "$path" "$attempt" "$smoke_retry_attempts" >&2
    if (( smoke_retry_delay_seconds > 0 )); then
      sleep "$smoke_retry_delay_seconds"
    fi
    attempt=$((attempt + 1))
  done
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

info_body="$(request GET "/actuator/info")"
if [[ "$info_body" != *'"name":"repl.us backend"'* ]]; then
  echo "info: app metadata missing from response" >&2
  exit 1
fi
echo "info: ok"

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
