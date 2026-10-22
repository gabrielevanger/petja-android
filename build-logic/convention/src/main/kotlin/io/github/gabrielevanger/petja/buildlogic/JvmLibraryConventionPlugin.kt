package io.github.gabrielevanger.petja.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/** Módulo Kotlin puro, sem Android: o compilador impede qualquer import de framework. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinToolchain()
            configureUnitTests()
            configureKtlint()
            configureKover(android = false)
        }
    }
}
