// Interface compartilhada pelas telas: formatação e links de contato.
plugins {
    id("petja.android.library")
    id("petja.android.compose")
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(libs.compose.material.icons.core)
}
