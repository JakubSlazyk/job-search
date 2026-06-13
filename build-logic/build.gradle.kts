plugins {
    `kotlin-dsl`
}

dependencies {
    // Plugin artifacts the convention plugins apply — versions pinned via the catalog.
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.allopen.gradlePlugin) // backs kotlin("plugin.spring") in spring-service
    implementation(libs.spring.boot.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.protobuf.gradlePlugin)

    // Exposes the generated `LibrariesForLibs` accessor so precompiled convention
    // plugins can reference the version catalog (`the<LibrariesForLibs>()`).
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
