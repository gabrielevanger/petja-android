#!/usr/bin/env python3
"""Resume o relatório XML do Kover numa tabela Markdown por módulo.

Uso:
  scripts/coverage-summary.py RELATORIO.xml [RELATORIO_DA_MAIN.xml]

Com o relatório da main, a tabela ganha a diferença de cada módulo. No CI, a saída vai para o
resumo da execução e para o comentário de cobertura do PR.
"""
import collections
import os
import sys
import xml.etree.ElementTree as ET

BASE_PACKAGE = "io/github/gabrielevanger/xodopet/"
# Profundidade do pacote que identifica o módulo: core/domain, feature/store...
MODULE_DEPTH = 2
# Marca invisível que o CI usa para achar e atualizar o comentário do PR em vez de criar outro.
COMMENT_MARKER = "<!-- xodo-coverage -->"
# Variação menor que isto aparece como estável, para arredondamento não virar alarme.
DELTA_NOISE = 0.05
PROJECT = "**Projeto**"


def counter(element, kind):
    for c in element.findall("counter"):
        if c.get("type") == kind:
            return int(c.get("covered")), int(c.get("missed"))
    return 0, 0


def ratio(covered, missed):
    total = covered + missed
    return 100 * covered / total if total else None


def read(path):
    """Porcentagem de linhas e ramos por módulo, e do projeto, a partir do XML do Kover."""
    root = ET.parse(path).getroot()
    sums = collections.defaultdict(lambda: {"LINE": [0, 0], "BRANCH": [0, 0]})
    for package in root.findall("package"):
        name = package.get("name").removeprefix(BASE_PACKAGE)
        module = "`" + "/".join(name.split("/")[:MODULE_DEPTH]) + "`"
        for kind in ("LINE", "BRANCH"):
            covered, missed = counter(package, kind)
            sums[module][kind][0] += covered
            sums[module][kind][1] += missed
    result = {module: {kind: ratio(*values) for kind, values in kinds.items()} for module, kinds in sums.items()}
    result[PROJECT] = {kind: ratio(*counter(root, kind)) for kind in ("LINE", "BRANCH")}
    return result


def cell(value):
    return "sem código" if value is None else f"{value:.1f}%"


def delta(current, base):
    if current is None:
        return "removido"
    if base is None:
        return "novo"
    change = current - base
    if abs(change) < DELTA_NOISE:
        return "="
    arrow = "▲" if change > 0 else "▼"
    return f"{arrow} {abs(change):.1f}"


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    current = read(sys.argv[1])
    base_path = sys.argv[2] if len(sys.argv) > 2 else None
    base = read(base_path) if base_path and os.path.exists(base_path) else None

    print(COMMENT_MARKER)
    print("## Cobertura de testes (Kover)\n")
    if base:
        print("| Módulo | Linhas | vs. main | Ramos | vs. main |")
        print("|---|---:|---:|---:|---:|")
    else:
        print("| Módulo | Linhas | Ramos |")
        print("|---|---:|---:|")

    modules = sorted(m for m in current if m != PROJECT) + [PROJECT]
    for module in modules:
        line, branch = current[module]["LINE"], current[module]["BRANCH"]
        bold = (lambda text: f"**{text}**") if module == PROJECT else (lambda text: text)
        if base:
            base_line = base.get(module, {}).get("LINE")
            base_branch = base.get(module, {}).get("BRANCH")
            print(
                f"| {module} | {bold(cell(line))} | {delta(line, base_line)} "
                f"| {bold(cell(branch))} | {delta(branch, base_branch)} |",
            )
        else:
            print(f"| {module} | {bold(cell(line))} | {bold(cell(branch))} |")

    print()
    if not base:
        print("Sem relatório da `main` para comparar ainda.\n")
    print(
        "Pisos: 90% das linhas no projeto, 95% das linhas e 70% dos ramos no `core/domain`, "
        "90% das linhas no `core/data`. Ramos de telas Compose ficam baixos por natureza: "
        "o compilador gera ramos internos que nenhum teste percorre.",
    )


if __name__ == "__main__":
    main()
