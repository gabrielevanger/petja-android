#!/usr/bin/env bash
set -euo pipefail
echo "Gravando screenshots de baseline (Roborazzi)..."
./gradlew test -Droborazzi.test.record=true
echo "Concluído. Imagens salvas em src/test/snapshots/"
