plugins {
    id("xodo.jvm.library")
    // Fixtures e fakes compartilhados com os testes de outros módulos, fora do código de produção.
    `java-test-fixtures`
}

dependencies {
    // Exposto na API pública do domínio (funções suspend e, mais adiante, Flow).
    api(libs.kotlinx.coroutines.core)
}
