plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ktlint)
    id("xodo.kover.aggregation")
}

// Aplicado na raiz para verificar também os scripts do Gradle deste diretório.
ktlint {
    version.set(libs.versions.ktlint)
}

// Módulos que entram no relatório de cobertura (Kover). Módulo novo: incluir aqui.
dependencies {
    kover(projects.app)
    kover(projects.core.domain)
    kover(projects.core.data)
    kover(projects.core.designsystem)
    kover(projects.core.ui)
    kover(projects.feature.storefront)
    kover(projects.feature.store)
    kover(projects.feature.cart)
    kover(projects.feature.account)
    kover(projects.feature.checkout)
    kover(projects.feature.orders)
}
