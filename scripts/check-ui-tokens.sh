#!/usr/bin/env bash
# Barra valores soltos nas telas: medidas, cores, estilos de texto e textos visíveis precisam vir
# dos tokens do core/designsystem (XodoTheme) e dos arquivos strings.xml.
#
# Fica de fora: o próprio core/designsystem (onde os tokens são definidos), os previews (*Preview.kt,
# com dados de exemplo) e os testes.
#
# Uso: scripts/check-ui-tokens.sh

set -euo pipefail
export LC_ALL=C.UTF-8

cd "$(dirname "$0")/.."

mapfile -t files < <(
  find app feature core -path '*/src/main/*' -name '*.kt' \
    -not -path 'core/designsystem/*' \
    -not -name '*Preview.kt' | sort
)

declare -a rules=(
  '[0-9]+(\.[0-9]+)?\.(dp|sp)\b|medida solta: use XodoTheme.spacing, sizes ou typography'
  'Color\(0x|cor solta: use um papel de XodoTheme.colors'
  'MaterialTheme\.(typography|shapes|colorScheme)|estilo direto do Material: use XodoTheme'
  'FontWeight\.|peso de fonte solto: use um estilo de XodoTheme.typography'
  '(Text\(|text = )"|texto visível fixo: use stringResource'
  'joinToString\(" |separador fixo: use dotSeparated'
)

status=0
for rule in "${rules[@]}"; do
  # O padrão pode conter "|"; a mensagem é o que vem depois do último "|".
  message="${rule##*|}"
  pattern="${rule%|*}"
  if matches=$(grep -nE "$pattern" "${files[@]}" 2>/dev/null); then
    while IFS= read -r line; do
      printf '%s\n  -> %s\n' "$line" "$message"
    done <<< "$matches"
    status=1
  fi
done

if (( status == 0 )); then
  echo "check-ui-tokens: nenhum valor solto nas telas (${#files[@]} arquivos)."
fi
exit $status
