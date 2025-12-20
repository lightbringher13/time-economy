#!/bin/sh
set -eu

NAME="auth-outbox-connector"
CONFIG_FILE="/connectors/auth-outbox-connector.config.json"
CONNECT_URL="http://connect:8083"

echo ">>> connector-init: starting"
echo ">>> connector-init: connector=$NAME"
echo ">>> connector-init: config=$CONFIG_FILE"

if [ ! -f "$CONFIG_FILE" ]; then
  echo "!!! config file not found: $CONFIG_FILE"
  ls -la /connectors || true
  exit 1
fi

echo ">>> Waiting for Kafka Connect..."
until curl -fsS "$CONNECT_URL/connectors" >/dev/null 2>&1; do
  sleep 2
done

echo ">>> Kafka Connect is up"

# if exists -> PUT config, else -> POST create
if curl -fsS "$CONNECT_URL/connectors/$NAME" >/dev/null 2>&1; then
  echo ">>> Connector exists, updating..."
  curl -fsS -X PUT \
    -H "Content-Type: application/json" \
    --data @"$CONFIG_FILE" \
    "$CONNECT_URL/connectors/$NAME/config" >/dev/null
else
  echo ">>> Connector missing, creating..."
  BODY="$(printf '{"name":"%s","config":%s}' "$NAME" "$(cat "$CONFIG_FILE")")"
  curl -fsS -X POST \
    -H "Content-Type: application/json" \
    -d "$BODY" \
    "$CONNECT_URL/connectors" >/dev/null
fi

echo ">>> Done."