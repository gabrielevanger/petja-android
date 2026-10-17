package io.github.gabrielevanger.xodopet.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** Tela de feature: library com Compose, Hilt, ViewModel e os módulos core que toda feature usa. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("xodo.android.library")
            pluginManager.apply("xodo.android.compose")
            pluginManager.apply("xodo.android.hilt")

            dependencies {
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("kotlinx-collections-immutable"))
            }
        }
    }
}
