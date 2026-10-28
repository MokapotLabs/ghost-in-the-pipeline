#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ "${1:-}" != "--no-build" ]]; then mvn -B -ntp install -DskipTests; fi
mkdir -p .work
# Only this credential is read, without shell evaluation or echoing it to logs.
if [[ -z "${OPENROUTER_API_KEY:-}" && -f .env.local ]]; then
  OPENROUTER_API_KEY="$(python3 scripts/local_env.py)"
  export OPENROUTER_API_KEY
fi
# Use a native deployment YAML override; no custom inference provider or proxy.
python3 - <<'PY'
from pathlib import Path
import os,json
s=Path('app/src/main/resources/pipeline.yaml').read_text()
s=s.replace('model: tev1:0.8b','model: '+json.dumps(os.getenv('GHOST_MODEL','tev1:0.8b')))
s=s.replace('model: tev1:latest','model: '+json.dumps(os.getenv('GHOST_LARGE_MODEL','tev1:latest')))
s=s.replace('model: ~typesafe/jev-latest','model: '+json.dumps(os.getenv('GHOST_JEV_MODEL','~typesafe/jev-latest')))
s=s.replace('baseUrl: http://localhost:11434','baseUrl: '+json.dumps(os.getenv('GHOST_MODEL_URL','http://localhost:11434')))
Path('.work/runtime-pipeline.yaml').write_text(s)
PY
export PIPELINE_CONFIG="$PWD/.work/runtime-pipeline.yaml"
export GHOST_REPLAY_PATH="${GHOST_REPLAY_PATH:-$PWD/target/replay}"
exec java -Dquarkus.http.port="${GHOST_PORT:-8087}" -jar app/target/quarkus-app/quarkus-run.jar
