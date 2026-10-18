// Interface compartilhada pelas telas: formatação e links de contato.
plugins {
    id("xodo.android.library")
    id("xodo.android.compose")
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(libs.compose.material.icons.core)
}
