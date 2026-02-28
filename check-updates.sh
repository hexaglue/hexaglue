#!/usr/bin/env bash
# check-updates.sh - Verifie les mises a jour disponibles pour les dependances et plugins Maven.
#
# Ne modifie aucun pom.xml. Necessite Maven sur le PATH.
# Usage: ./check-updates.sh
#
# Options:
#   -v, --verbose    Affiche la sortie Maven complete (sans filtrage)
#   -h, --help       Affiche cette aide

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGIN="org.codehaus.mojo:versions-maven-plugin:2.21.0"
RULES_URI="file://${SCRIPT_DIR}/build/versions-rules.xml"

# Filtre les versions pre-release : alpha, beta, RC, milestone (M1, M2...)
IGNORE_PATTERN='(?i).*-(alpha|beta|rc|m|snapshot)([-.]?[0-9]+)?|.*\.M[0-9]+'

VERBOSE=false

usage() {
    sed -n '2,8s/^# \?//p' "$0"
    exit 0
}

for arg in "$@"; do
    case "$arg" in
        -v|--verbose) VERBOSE=true ;;
        -h|--help) usage ;;
        *) echo "Option inconnue: $arg"; usage ;;
    esac
done

echo "=== HexaGlue : verification des mises a jour ==="
echo "Filtre : versions stables uniquement (pas de alpha/beta/RC/milestone)"
echo ""

cd "${SCRIPT_DIR}"

run_goal() {
    local goal="$1"
    local label="$2"

    echo "--- ${label} ---"
    echo ""

    if $VERBOSE; then
        mvn "${PLUGIN}:${goal}" \
            "-Dmaven.version.ignore=${IGNORE_PATTERN}" \
            "-Dmaven.version.rules=${RULES_URI}" \
            -q
    else
        local output
        output=$(mvn "${PLUGIN}:${goal}" \
            "-Dmaven.version.ignore=${IGNORE_PATTERN}" \
            "-Dmaven.version.rules=${RULES_URI}" \
            2>&1 || true)

        local updates
        updates=$(echo "$output" | grep -E '^\[INFO\].*->' | sort -u || true)

        if [ -n "$updates" ]; then
            echo "$updates"
        else
            echo "(aucune mise a jour disponible)"
        fi
    fi

    echo ""
}

run_goal "display-property-updates" "Mises a jour des properties (versions centralisees)"
run_goal "display-dependency-updates" "Mises a jour des dependances (versions hardcodees)"

echo "Termine. Aucun fichier modifie."
