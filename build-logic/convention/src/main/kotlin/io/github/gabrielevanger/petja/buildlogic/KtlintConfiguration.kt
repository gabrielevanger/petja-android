package io.github.gabrielevanger.petja.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/** Regras de estilo ficam no .editorconfig da raiz, lido pelo ktlint e pelo Android Studio. */
internal fun Project.configureKtlint() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    extensions.configure<KtlintExtension> {
        version.set(libs.versionOf("ktlint"))
    }
}
