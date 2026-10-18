#!/usr/bin/env python3
"""Resume o relatório XML do Kover numa tabela Markdown por módulo.

Uso: scripts/coverage-summary.py [build/reports/kover/reportCoverage.xml]
No CI, a saída vai para o resumo da execução ($GITHUB_STEP_SUMMARY).
"""
import collections
import sys
import xml.etree.ElementTree as ET

BASE_PACKAGE = "io/github/gabrielevanger/xodopet/"
# Profundidade do pacote que identifica o módulo: core/domain, feature/store...
MODULE_DEPTH = 2


def counter(element, kind):
    for c in element.findall("counter"):
        if c.get("type") == kind:
            return int(c.get("covered")), int(c.get("missed"))
    return 0, 0


def percent(covered, missed):
    total = covered + missed
    return f"{100 * covered / total:.1f}%" if total else "sem código"


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "build/reports/kover/reportCoverage.xml"
    root = ET.parse(path).getroot()
    modules = collections.defaultdict(lambda: {"LINE": [0, 0], "BRANCH": [0, 0]})
    for package in root.findall("package"):
        name = package.get("name").removeprefix(BASE_PACKAGE)
        module = "/".join(name.split("/")[:MODULE_DEPTH])
        for kind in ("LINE", "BRANCH"):
            covered, missed = counter(package, kind)
            modules[module][kind][0] += covered
            modules[module][kind][1] += missed

    print("## Cobertura de testes (Kover)\n")
    print("| Módulo | Linhas | Ramos |")
    print("|---|---:|---:|")
    for module, kinds in sorted(modules.items()):
        print(f"| `{module}` | {percent(*kinds['LINE'])} | {percent(*kinds['BRANCH'])} |")
    print(f"| **Projeto** | **{percent(*counter(root, 'LINE'))}** | **{percent(*counter(root, 'BRANCH'))}** |")
    print("\nRamos de telas Compose ficam baixos por natureza: o compilador gera ramos internos que nenhum teste percorre.")


if __name__ == "__main__":
    main()
