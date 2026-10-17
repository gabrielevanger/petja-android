import java.util.Properties

plugins {
    id("xodo.android.application")
    id("xodo.android.compose")
    id("xodo.android.hilt")
}

// Projeto Supabase: supabase.url e supabase.anonKey no local.properties (fora do Git),
// ou -Psupabase.url=... -Psupabase.anonKey=... na linha de comando. Ver README.
val localProperties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull?.let { load(it.reader()) }
}

fun supabaseProperty(name: String): String = providers.gradleProperty(name).orNull
    ?: localProperties.getProperty(name)
    ?: ""

android {
    defaultConfig {
        applicationId = "io.github.gabrielevanger.xodopet"
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseProperty("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseProperty("supabase.anonKey")}\"")
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.feature.storefront)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}
