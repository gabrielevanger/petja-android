package io.github.gabrielevanger.xodopet.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                defaultConfig.targetSdk = libs.intVersionOf("targetSdk")
                lint {
                    checkDependencies = true
                    warningsAsErrors = true
                    abortOnError = true
                    // Estas checagens consultam versões novas na internet: o resultado mudaria
                    // sem nenhuma alteração no código. Atualizar dependências é tarefa à parte.
                    disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
                }
            }
            configureUnitTests()
            configureKtlint()
            configureKover(android = true, verifyMinimum = false)
        }
    }
}
