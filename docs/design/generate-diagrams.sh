#!/usr/bin/env bash
# =============================================================================
# Render Mermaid + PlantUML diagrams to docs/exports/
# Usage: ./generate-diagrams.sh [png|pdf|both]
# =============================================================================

set -euo pipefail

FORMAT="${1:-png}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXPORT_DIR="$(dirname "$SCRIPT_DIR")/exports"

mkdir -p "$EXPORT_DIR"

echo "=== Utility Billing System — Diagram Export ==="
echo "Source : $SCRIPT_DIR"
echo "Output : $EXPORT_DIR"
echo ""

MERMAID_FILES=(
  "erd.mmd"
  "system-flow.mmd"
  "simple-workflow.mmd"
  "simple-roles.mmd"
  "bill-lifecycle.mmd"
)

PLANTUML_FILES=(
  "erd.puml"
  "system-flow.puml"
)

export_mermaid() {
  local file="$1"
  local ext="$2"
  local base="${file%.mmd}"
  local input="$SCRIPT_DIR/$file"
  local output="$EXPORT_DIR/${base}.${ext}"

  echo "  Mermaid: $file -> ${base}.${ext}"
  if [[ "$ext" == "pdf" ]]; then
    npx --yes @mermaid-js/mermaid-cli -i "$input" -o "$output" -b transparent -e pdf
  else
    npx --yes @mermaid-js/mermaid-cli -i "$input" -o "$output" -b transparent -w 1920 -H 1080
  fi
}

if command -v node >/dev/null 2>&1; then
  echo "[Mermaid CLI]"
  for file in "${MERMAID_FILES[@]}"; do
    [[ -f "$SCRIPT_DIR/$file" ]] || { echo "Missing: $file"; continue; }
    [[ "$FORMAT" == "png" || "$FORMAT" == "both" ]] && export_mermaid "$file" png
    [[ "$FORMAT" == "both" ]] && export_mermaid "$file" pdf
  done
else
  echo "WARN: Node.js not found. Use https://mermaid.live for .mmd files."
fi

echo ""

export_plantuml() {
  local file="$1"
  local ext="$2"
  local input="$SCRIPT_DIR/$file"
  echo "  PlantUML: $file -> *.${ext}"

  if [[ -f "$SCRIPT_DIR/plantuml.jar" ]]; then
    java -jar "$SCRIPT_DIR/plantuml.jar" "-t${ext}" -o "$EXPORT_DIR" "$input"
  elif command -v plantuml >/dev/null 2>&1; then
    plantuml "-t${ext}" -o "$EXPORT_DIR" "$input"
  else
    echo "WARN: PlantUML not found. Download plantuml.jar to docs/design/ or use plantuml.com"
    return 1
  fi
}

if command -v java >/dev/null 2>&1; then
  echo "[PlantUML]"
  for file in "${PLANTUML_FILES[@]}"; do
    [[ -f "$SCRIPT_DIR/$file" ]] || { echo "Missing: $file"; continue; }
    [[ "$FORMAT" == "png" || "$FORMAT" == "both" ]] && export_plantuml "$file" png || true
    [[ "$FORMAT" == "both" ]] && export_plantuml "$file" pdf || true
  done
fi

echo ""
echo "Done. Exported files:"
ls -la "$EXPORT_DIR"
