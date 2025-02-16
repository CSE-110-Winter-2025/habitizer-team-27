plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "edu.ucsd.cse110.habitizer.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "edu.ucsd.cse110.habitizer.app"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

}

dependencies {

    implementation(libs.android.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":lib"))
    implementation(project(":observables"))
    implementation(project(":observables"))

    testImplementation(libs.junit4)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.robolectric:robolectric:4.10.3")

    // AndroidX Test
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation ("org.robolectric:robolectric:4.9")
    testImplementation(libs.androidx.test.ext.junit)


    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.espresso.core)
}
