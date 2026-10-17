dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // O artefato do plugin do ktlint só é publicado no Gradle Plugin Portal.
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
