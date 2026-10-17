package io.github.gabrielevanger.xodopet.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/**
 * Habilita Compose e os testes de interface na JVM com Robolectric, que rodam no CI junto com os
 * unitários, sem emulador. Deve ser aplicado depois de xodo.android.library ou xodo.android.application.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.getByType<CommonExtension>().apply {
                buildFeatures.compose = true
                // Robolectric precisa dos recursos do módulo (strings, ícones) para renderizar o Compose na JVM.
                // Só em módulos com testes: o arquivo gerado por esta opção faz o Gradle esperar testes
                // num módulo que não tem nenhum, e a tarefa de teste falha.
                testOptions.unitTests.isIncludeAndroidResources = file("src/test").exists()
            }

            // O Robolectric acessa internos do JDK ao simular arquivos do Android; o Java 21 exige liberação explícita.
            tasks.withType<Test>().configureEach {
                jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
            }

            dependencies {
                val bom = platform(libs.library("compose-bom"))
                add("implementation", bom)
                add("androidTestImplementation", bom)
                add("testImplementation", bom)
                add("implementation", libs.library("compose-ui"))
                add("implementation", libs.library("compose-ui-graphics"))
                add("implementation", libs.library("compose-ui-tooling-preview"))
                add("implementation", libs.library("compose-material3"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("debugImplementation", libs.library("compose-ui-tooling"))
                add("debugImplementation", libs.library("compose-ui-test-manifest"))
                add("androidTestImplementation", libs.library("compose-ui-test-junit4"))

                add("testImplementation", libs.library("compose-ui-test-junit4"))
                add("testImplementation", libs.library("robolectric"))
                add("testImplementation", libs.library("androidx-test-core"))
                add("testImplementation", libs.library("androidx-test-ext-junit"))
                // Os testes do Robolectric usam JUnit 4; o motor vintage os executa na plataforma do JUnit 5.
                add("testRuntimeOnly", libs.library("junit-vintage-engine"))
            }
        }
    }
}
