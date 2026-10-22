package io.github.gabrielevanger.petja.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Tela de feature: library com Compose, Hilt, ViewModel, navegação com rotas tipadas
 * (classes @Serializable) e os módulos core que toda feature usa.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("petja.android.library")
            pluginManager.apply("petja.android.compose")
            pluginManager.apply("petja.android.hilt")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("kotlinx-collections-immutable"))
                add("implementation", libs.library("androidx-navigation-compose"))
                add("implementation", libs.library("compose-material-icons-core"))
            }
        }
    }
}
