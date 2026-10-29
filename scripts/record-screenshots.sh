#!/usr/bin/env bash
# Grava (ou regrava) as imagens de referência dos screenshot tests do Roborazzi.
# Rode depois de mudar uma tela de propósito e confira as imagens antes do commit.
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew recordRoborazziDebug
echo "Imagens salvas em <módulo>/src/test/snapshots/"
