plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // This library contains no executable code, so it is
    // safe to use even in a non-Android library module.
    implementation(libs.androidx.annotations)
    
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.hamcrest)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}