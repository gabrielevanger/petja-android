pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Baixa o JDK exigido pelo toolchain quando a máquina não o tem instalado.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "xodo-pet"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
include(":feature:storefront")
include(":feature:store")
include(":feature:cart")
include(":feature:account")
include(":feature:checkout")
