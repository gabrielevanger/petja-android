package io.github.gabrielevanger.petja.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // O AGP 9 já compila Kotlin: o plugin kotlin-android não é mais aplicado.
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> {
                configureAndroid(this)
            }
            configureUnitTests()
            configureKtlint()
            configureKover(android = true)
        }
    }
}
