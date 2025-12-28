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

echo ">>> Applying connector config (create-or-update via PUT)..."
curl -fsS -X PUT \
  -H "Content-Type: application/json" \
  --data @"$CONFIG_FILE" \
  "$CONNECT_URL/connectors/$NAME/config" >/dev/null

echo ">>> Connector status:"
curl -sS "$CONNECT_URL/connectors/$NAME/status" || true
echo
echo ">>> Done."