package io.github.gabrielevanger.petja.buildlogic

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Aplicado na raiz: junta a cobertura dos módulos listados com `kover(...)` no build.gradle.kts da raiz
 * num relatório só.
 *
 * Relatórios: ./gradlew koverHtmlReportCoverage (build/reports/kover/htmlCoverage/index.html)
 * Piso:       ./gradlew koverVerifyCoverage
 */
class KoverAggregationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")
            extensions.configure<KoverProjectExtension> {
                currentProject {
                    // A raiz não tem código; a variante vazia recebe as dos módulos.
                    createVariant(COVERAGE_VARIANT) {}
                }
                reports {
                    filters { excludeGeneratedCode() }
                    variant(COVERAGE_VARIANT) {
                        verify { rule("Linhas no projeto") { minBound(PROJECT_MIN_LINES, CoverageUnit.LINE) } }
                    }
                }
            }
        }
    }
}

/** Piso do projeto inteiro: abaixo disso o CI falha. Os módulos de regra têm pisos próprios, mais altos. */
private const val PROJECT_MIN_LINES = 90
