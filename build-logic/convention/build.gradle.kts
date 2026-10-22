plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set(libs.versions.ktlint)
}

group = "io.github.gabrielevanger.petja.buildlogic"

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
            id = "petja.jvm.library"
            implementationClass = "io.github.gabrielevanger.petja.buildlogic.JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "petja.android.library"
            implementationClass = "io.github.gabrielevanger.petja.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "petja.android.application"
            implementationClass = "io.github.gabrielevanger.petja.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "petja.android.compose"
            implementationClass = "io.github.gabrielevanger.petja.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "petja.android.hilt"
            implementationClass = "io.github.gabrielevanger.petja.buildlogic.AndroidHiltConventionPlugin"
        }
        register("koverAggregation") {
            id = "petja.kover.aggregation"
            implementationClass = "io.github.gabrielevanger.petja.buildlogic.KoverAggregationConventionPlugin"
        }
        register("androidFeature") {
            id = "petja.android.feature"
            implementationClass = "io.github.gabrielevanger.petja.buildlogic.AndroidFeatureConventionPlugin"
        }
    }
}
