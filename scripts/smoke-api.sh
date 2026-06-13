#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/smoke-api.sh [--with-guest-auth] [--with-room-flow] <api-base-url>
  API_BASE_URL=<api-base-url> scripts/smoke-api.sh [--with-guest-auth] [--with-room-flow]

Checks:
  - /actuator/health/liveness
  - /actuator/health/readiness
  - /actuator/info
  - /api/auth/guest and /api/me when --with-guest-auth is provided
  - room create, invite join, and /api/rooms/{roomId}/today when --with-room-flow is provided

Examples:
  scripts/smoke-api.sh https://api.example.com
  scripts/smoke-api.sh --with-guest-auth https://api.example.com
  scripts/smoke-api.sh --with-room-flow https://api.example.com

Environment:
  SMOKE_CONNECT_TIMEOUT_SECONDS  Curl connect timeout. Defaults to 5.
  SMOKE_CURL_TIMEOUT_SECONDS     Per-request curl timeout. Defaults to 20.
  SMOKE_RETRY_ATTEMPTS           Attempts per request. Defaults to 1.
  SMOKE_RETRY_DELAY_SECONDS      Delay between retries. Defaults to 2.
  SMOKE_CLEANUP_TOKEN            Optional operations token for smoke room cleanup.
EOF
}

with_guest_auth=false
with_room_flow=false
base_url="${API_BASE_URL:-}"
smoke_connect_timeout_seconds="${SMOKE_CONNECT_TIMEOUT_SECONDS:-5}"
smoke_curl_timeout_seconds="${SMOKE_CURL_TIMEOUT_SECONDS:-20}"
smoke_retry_attempts="${SMOKE_RETRY_ATTEMPTS:-1}"
smoke_retry_delay_seconds="${SMOKE_RETRY_DELAY_SECONDS:-2}"
smoke_cleanup_token="${SMOKE_CLEANUP_TOKEN:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-guest-auth)
      with_guest_auth=true
      shift
      ;;
    --with-room-flow)
      with_room_flow=true
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

extract_json_string() {
  local field="$1"
  tr -d '\n' | sed -nE "s/.*\"$field\"[[:space:]]*:[[:space:]]*\"([^\"]+)\".*/\1/p" | head -n 1
}

extract_top_level_id() {
  tr -d '\n' | sed -nE 's/^[[:space:]]*\{[[:space:]]*"id"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/p' | head -n 1
}

extract_today_mission_id() {
  tr -d '\n' | sed -nE 's/.*"mission"[[:space:]]*:[[:space:]]*\{[[:space:]]*"id"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/p' | head -n 1
}

require_value() {
  local label="$1"
  local value="$2"

  if [[ -z "$value" ]]; then
    echo "$label: expected value, got empty response field" >&2
    exit 1
  fi
}

require_body_contains() {
  local label="$1"
  local body="$2"
  local expected="$3"

  if [[ "$body" != *"$expected"* ]]; then
    echo "$label: expected response to contain $expected, got $body" >&2
    exit 1
  fi
}

create_guest_session() {
  local display_name="$1"
  local body
  local access_token

  body="$(
    request POST "/api/auth/guest" \
      -H "Content-Type: application/json" \
      --data "{\"displayName\":\"$display_name\"}"
  )"
  access_token="$(printf '%s' "$body" | extract_access_token)"
  require_value "guest auth" "$access_token"
  printf '%s' "$access_token"
}

run_room_flow() {
  local owner_access_token="$1"
  local timestamp
  local room_name
  local room_body
  local room_id
  local invite_body
  local invite_code
  local room_id_prefix
  local member_access_token
  local join_body
  local member_id
  local today_body
  local mission_id
  local prompt
  local update_body
  local cleanup_body
  local room_cleanup_body

  timestamp="$(date -u +%Y%m%d%H%M%S)"
  room_name="Smoke Room $timestamp"

  room_body="$(
    request POST "/api/rooms" \
      -H "Authorization: Bearer $owner_access_token" \
      -H "Content-Type: application/json" \
      --data "{\"name\":\"$room_name\"}"
  )"
  room_id="$(printf '%s' "$room_body" | extract_top_level_id)"
  require_value "room create" "$room_id"
  require_body_contains "room create" "$room_body" '"currentUserRole":"OWNER"'
  echo "room create: ok"

  room_body="$(
    request GET "/api/rooms/$room_id" \
      -H "Authorization: Bearer $owner_access_token"
  )"
  require_body_contains "room detail" "$room_body" "\"id\":\"$room_id\""
  require_body_contains "room detail" "$room_body" '"currentUserRole":"OWNER"'
  echo "room detail: ok"

  invite_body="$(
    request POST "/api/rooms/$room_id/invite-links" \
      -H "Authorization: Bearer $owner_access_token" \
      -H "Content-Type: application/json" \
      --data '{"expiresInHours":1,"maxUses":1,"rotate":true}'
  )"
  invite_code="$(printf '%s' "$invite_body" | extract_json_string "code")"
  require_value "invite link" "$invite_code"
  room_id_prefix="${room_id:0:8}"
  if [[ "$invite_code" == *"$room_id_prefix"* ]]; then
    echo "invite link: expected opaque code, got $invite_code" >&2
    exit 1
  fi
  echo "invite link: ok"

  member_access_token="$(create_guest_session "Smoke Member")"
  join_body="$(
    request POST "/api/invite-links/$invite_code/join" \
      -H "Authorization: Bearer $member_access_token"
  )"
  member_id="$(printf '%s' "$join_body" | extract_json_string "currentUserMemberId")"
  require_value "invite join" "$member_id"
  require_body_contains "invite join" "$join_body" "\"id\":\"$room_id\""
  require_body_contains "invite join" "$join_body" '"currentUserRole":"MEMBER"'
  require_body_contains "invite join" "$join_body" '"memberCount":2'
  echo "invite join: ok"

  today_body="$(
    request GET "/api/rooms/$room_id/today" \
      -H "Authorization: Bearer $owner_access_token"
  )"
  mission_id="$(printf '%s' "$today_body" | extract_today_mission_id)"
  require_value "today mission" "$mission_id"
  require_body_contains "today mission" "$today_body" "\"id\":\"$room_id\""
  require_body_contains "today mission" "$today_body" '"canEdit":true'
  echo "today mission: ok"

  prompt="Smoke prompt"
  update_body="$(
    request PATCH "/api/rooms/$room_id/missions/$mission_id" \
      -H "Authorization: Bearer $owner_access_token" \
      -H "Content-Type: application/json" \
      --data "{\"prompt\":\"$prompt\",\"category\":\"OBSERVATION\"}"
  )"
  require_body_contains "mission update" "$update_body" "\"id\":\"$mission_id\""
  require_body_contains "mission update" "$update_body" "\"prompt\":\"$prompt\""
  require_body_contains "mission update" "$update_body" '"editCount":1'
  echo "mission update: ok"

  cleanup_body="$(
    request DELETE "/api/rooms/$room_id/members/$member_id" \
      -H "Authorization: Bearer $owner_access_token"
  )"
  require_body_contains "member cleanup" "$cleanup_body" "\"memberId\":\"$member_id\""
  require_body_contains "member cleanup" "$cleanup_body" '"status":"REMOVED"'
  echo "member cleanup: ok"

  if [[ -n "$smoke_cleanup_token" ]]; then
    room_cleanup_body="$(
      request DELETE "/internal/operations/smoke-rooms/$room_id" \
        -H "X-Replus-Operations-Token: $smoke_cleanup_token"
    )"
    require_body_contains "room cleanup" "$room_cleanup_body" "\"roomId\":\"$room_id\""
    require_body_contains "room cleanup" "$room_cleanup_body" '"deleted":true'
    echo "room cleanup: ok"
  else
    echo "room cleanup: skipped"
  fi
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
  access_token="$(create_guest_session "Smoke Test")"
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

  if [[ "$with_room_flow" == "true" ]]; then
    run_room_flow "$access_token"
  fi
fi
