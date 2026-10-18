plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set(libs.versions.ktlint)
}

group = "io.github.gabrielevanger.xodopet.buildlogic"

kotlin {
    jvmToolchain(
        libs.versions.jvmToolchain
            .get()
            .toInt(),
    )
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    // implementation, e não compileOnly: nenhum outro build traz o Kover para o classpath dos projetos.
    implementation(libs.kover.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "xodo.jvm.library"
            implementationClass = "io.github.gabrielevanger.xodopet.buildlogic.JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "xodo.android.library"
            implementationClass = "io.github.gabrielevanger.xodopet.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "xodo.android.application"
            implementationClass = "io.github.gabrielevanger.xodopet.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "xodo.android.compose"
            implementationClass = "io.github.gabrielevanger.xodopet.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "xodo.android.hilt"
            implementationClass = "io.github.gabrielevanger.xodopet.buildlogic.AndroidHiltConventionPlugin"
        }
        register("koverAggregation") {
            id = "xodo.kover.aggregation"
            implementationClass = "io.github.gabrielevanger.xodopet.buildlogic.KoverAggregationConventionPlugin"
        }
        register("androidFeature") {
            id = "xodo.android.feature"
            implementationClass = "io.github.gabrielevanger.xodopet.buildlogic.AndroidFeatureConventionPlugin"
        }
    }
}
