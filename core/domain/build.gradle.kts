import io.github.gabrielevanger.xodopet.buildlogic.minimumCoverage

plugins {
    id("xodo.jvm.library")
    // Fixtures e fakes compartilhados com os testes de outros módulos, fora do código de produção.
    `java-test-fixtures`
}

dependencies {
    // Exposto na API pública do domínio (funções suspend e, mais adiante, Flow).
    api(libs.kotlinx.coroutines.core)
}

// Regras de negócio: o piso mais alto do projeto (ver CONTRIBUTING).
minimumCoverage(lines = 95, branches = 70)
