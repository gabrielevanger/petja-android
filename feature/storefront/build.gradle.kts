plugins {
    id("xodo.android.feature")
}

dependencies {
    testImplementation(testFixtures(projects.core.domain))
}
