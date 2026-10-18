import io.github.gabrielevanger.xodopet.buildlogic.minimumCoverage

plugins {
    id("xodo.android.library")
    id("xodo.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.datastore)

    testImplementation(libs.ktor.client.mock)
    testImplementation(testFixtures(projects.core.domain))
}

minimumCoverage(lines = 90)
