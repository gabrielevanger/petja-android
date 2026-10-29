import io.github.gabrielevanger.petja.buildlogic.minimumCoverage

plugins {
    id("petja.jvm.library")
    // Fixtures e fakes compartilhados com os testes de outros módulos, fora do código de produção.
    `java-test-fixtures`
}

dependencies {
    // Exposto na API pública do domínio (funções suspend e, mais adiante, Flow).
    api(libs.kotlinx.coroutines.core)
    // Testes arquiteturais (ArquiteturaTest): leem o código de todos os módulos.
    testImplementation(libs.konsist)
}

// O Konsist lê o código do projeto inteiro, não só deste módulo. Sem estas entradas o Gradle
// consideraria o teste em dia (ou o traria do cache) quando só uma feature muda.
tasks.test {
    inputs.files(
        fileTree(rootDir) {
            include("*/src/**/*.kt", "*/*/src/**/*.kt")
            exclude("build-logic/**")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("codigoDoProjeto")
}

// Regras de negócio: o piso mais alto do projeto (ver CONTRIBUTING).
minimumCoverage(lines = 95, branches = 70)
