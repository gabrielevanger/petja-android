plugins {
    id("petja.android.feature")
}

dependencies {
    testImplementation(testFixtures(projects.core.domain))
}
