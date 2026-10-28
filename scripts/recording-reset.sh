#!/usr/bin/env bash
set -euo pipefail
curl --fail --silent --show-error -X POST "http://127.0.0.1:${GHOST_PORT:-8087}/api/game" -H 'Content-Type: application/json' -d '{"seed":1983,"level":1,"singleLevel":false}' >/dev/null
printf 'Ready for recording at http://127.0.0.1:%s — ordinary seed 1983, human controls.\n' "${GHOST_PORT:-8087}"
