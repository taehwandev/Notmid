#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

status=0

is_allowed_template() {
  case "$1" in
    *.example|*.sample|*.template|*.dist|*.defaults|*.default|*.schema)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

is_forbidden_secret_path() {
  local path="$1"
  local base="${path##*/}"

  case "$base" in
    local.properties|secrets.properties|google-services.json|GoogleService-Info.plist)
      return 0
      ;;
    *.jks|*.keystore|*.p12|*.p8|*.pem|*.key)
      return 0
      ;;
  esac

  if [[ "$base" == firebase-adminsdk-*.json ||
    "$base" == *service-account*.json ||
    "$base" == *serviceAccount*.json ]]; then
    return 0
  fi

  if [[ "$base" == .env || "$base" == .env.* ]]; then
    if is_allowed_template "$base"; then
      return 1
    fi
    return 0
  fi

  return 1
}

is_example_config_file() {
  case "$1" in
    *.env.example|*.env.sample|*.env.template|*.env.dist|*.env.defaults|*.env.default|*.env.schema|local.properties.example)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

is_sensitive_placeholder_key() {
  case "$1" in
    *SECRET*|*PASSWORD*|*TOKEN*|*PRIVATE*|*SERVICE_ACCOUNT*|*KEYSTORE*|*STORE_FILE*|*KEY_ALIAS*|*API_KEY*|*ACCESS_KEY*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

echo "== Forbidden secret files =="
while IFS= read -r path; do
  if is_forbidden_secret_path "$path"; then
    echo "Forbidden secret/config file is visible to git: ${path}" >&2
    status=1
  fi
done < <(git ls-files --cached --others --exclude-standard)

echo "== Example config placeholders =="
while IFS= read -r path; do
  if [[ ! -f "$path" ]] || ! is_example_config_file "$path"; then
    continue
  fi

  line_number=0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line_number=$((line_number + 1))
    [[ "$line" =~ ^[[:space:]]*$ || "$line" =~ ^[[:space:]]*# ]] && continue
    [[ "$line" != *=* ]] && continue

    key="${line%%=*}"
    value="${line#*=}"
    key="${key#"${key%%[![:space:]]*}"}"
    key="${key%"${key##*[![:space:]]}"}"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"

    if is_sensitive_placeholder_key "$key" && [[ -n "$value" ]]; then
      echo "Sensitive example config value must stay empty: ${path}:${line_number} ${key}" >&2
      status=1
    fi
  done < "$path"
done < <(git ls-files --cached --others --exclude-standard)

if [[ "$status" -ne 0 ]]; then
  echo "Secret hygiene failed. Move real values to ignored local files or CI secrets." >&2
  exit "$status"
fi

echo "Secret hygiene passed."
