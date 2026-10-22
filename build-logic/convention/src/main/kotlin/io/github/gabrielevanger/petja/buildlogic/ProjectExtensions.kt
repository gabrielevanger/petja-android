package io.github.gabrielevanger.petja.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

private const val BASE_PACKAGE = "io.github.gabrielevanger.petja"

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.versionOf(alias: String): String = findVersion(alias).get().requiredVersion

internal fun VersionCatalog.intVersionOf(alias: String): Int = versionOf(alias).toInt()

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias).get()

/**
 * Deriva o namespace do caminho do módulo, para não repeti-lo em cada build script:
 * `:core:domain` vira `io.github.gabrielevanger.petja.core.domain`.
 */
internal fun Project.namespaceFromPath(): String {
    val segments = path.removePrefix(":").split(":").map { it.replace("-", "") }
    return (listOf(BASE_PACKAGE) + segments).joinToString(".")
}
