package io.github.gabrielevanger.xodopet.buildlogic

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Variante de cobertura comum a todos os módulos. Nos módulos Android ela usa só os testes de debug:
 * sem isso o Kover rodaria também os de release, o dobro do tempo para o mesmo resultado.
 */
internal const val COVERAGE_VARIANT = "coverage"

/** Mínimo de linhas cobertas por testes em todo módulo com código (regra do projeto, ver CONTRIBUTING). */
internal const val MODULE_MIN_LINES = 80

/**
 * @param verifyMinimum false só no app, que tem apenas Activity e Application, fora da conta da cobertura.
 */
internal fun Project.configureKover(android: Boolean, verifyMinimum: Boolean = true) {
    pluginManager.apply("org.jetbrains.kotlinx.kover")
    extensions.configure<KoverProjectExtension> {
        currentProject {
            createVariant(COVERAGE_VARIANT) {
                if (android) add("debug") else add("jvm")
            }
        }
        reports {
            filters { excludeGeneratedCode() }
            if (verifyMinimum) {
                variant(COVERAGE_VARIANT) {
                    verify { rule("Mínimo do projeto") { minBound(MODULE_MIN_LINES, CoverageUnit.LINE) } }
                }
            }
        }
    }
}

/**
 * Piso de cobertura acima do mínimo, para módulos de regra de negócio, conferido por `./gradlew koverVerifyCoverage`.
 * Ramos só fazem sentido onde não há Compose: o compilador do Compose gera ramos internos
 * que nenhum teste de tela percorre.
 */
fun Project.minimumCoverage(lines: Int, branches: Int? = null) {
    extensions.configure<KoverProjectExtension> {
        reports {
            variant(COVERAGE_VARIANT) {
                verify {
                    rule("Linhas") { minBound(lines, CoverageUnit.LINE) }
                    if (branches != null) rule("Ramos") { minBound(branches, CoverageUnit.BRANCH) }
                }
            }
        }
    }
}

/** Código gerado ou só de ligação: não tem regra para testar e distorce a porcentagem. */
internal fun KoverReportFiltersConfig.excludeGeneratedCode() {
    excludes {
        classes(
            // Android e Compose
            "*.BuildConfig",
            "*.R",
            "*.R$*",
            "*.ComposableSingletons*",
            "*PreviewKt",
            "*PreviewKt$*",
            // Hilt e Dagger
            "*_Factory",
            "*_Factory$*",
            "*_MembersInjector",
            "*_HiltModules*",
            "*_GeneratedInjector",
            "*Hilt_*",
            "hilt_aggregated_deps.*",
            "dagger.hilt.internal.*",
            // kotlinx.serialization
            "*$\$serializer",
            // Rotas de navegação: só ligam telas, sem regra
            "*NavigationKt",
            "*NavigationKt$*",
            "*Destination",
            "*Destination$*",
        )
        // Injeção e app (Activity, Application) só ligam as peças; DTOs só espelham o JSON, e os ramos
        // deles são do código que o kotlinx.serialization gera dentro da própria classe.
        packages("*.di", "*.app", "*.dto")
        annotatedBy(
            "androidx.compose.ui.tooling.preview.Preview",
            "dagger.Module",
            "dagger.hilt.android.HiltAndroidApp",
            "dagger.hilt.android.AndroidEntryPoint",
        )
    }
}
