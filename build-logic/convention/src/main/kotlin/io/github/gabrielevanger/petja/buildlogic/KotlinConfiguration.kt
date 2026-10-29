package io.github.gabrielevanger.petja.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

internal fun Project.configureKotlinToolchain() {
    extensions.configure<KotlinBaseExtension> {
        jvmToolchain(libs.intVersionOf("jvmToolchain"))
    }
}

internal fun Project.configureAndroid(extension: CommonExtension) {
    val javaVersion = JavaVersion.toVersion(libs.intVersionOf("jvmToolchain"))
    extension.apply {
        namespace = namespaceFromPath()
        compileSdk = libs.intVersionOf("compileSdk")
        defaultConfig.minSdk = libs.intVersionOf("minSdk")
        compileOptions.sourceCompatibility = javaVersion
        compileOptions.targetCompatibility = javaVersion
    }
    configureKotlinToolchain()
}

internal fun Project.configureUnitTests() {
    dependencies {
        add("testImplementation", platform(libs.library("junit-bom")))
        add("testImplementation", libs.library("junit-jupiter"))
        add("testRuntimeOnly", libs.library("junit-platform-launcher"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testImplementation", libs.library("turbine"))
        // Konsist: testes arquiteturais que garantem as convenções do projeto (ex.: camadas, nomes, imports)
        add("testImplementation", libs.library("konsist"))
    }
    // O código compila para Java 17; os testes executam num JVM mais novo, exigido pelo Robolectric.
    val testLauncher = extensions.getByType<JavaToolchainService>().launcherFor {
        languageVersion.set(JavaLanguageVersion.of(libs.intVersionOf("testJvm")))
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        javaLauncher.set(testLauncher)
        System.getProperties().stringPropertyNames()
            .filter { it.startsWith("roborazzi") }
            .forEach { key -> systemProperty(key, System.getProperty(key).orEmpty()) }
    }
}
