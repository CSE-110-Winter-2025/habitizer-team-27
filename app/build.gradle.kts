plugins {
    alias(libs.plugins.android.application)
}

android {
<<<<<<< HEAD
    namespace = "edu.ucsd.cse110.habitizer.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "edu.ucsd.cse110.habitizer.app"
        minSdk = 34
=======
    namespace = "edu.ucsd.cse110.secards.habitizer_test"
    compileSdk = 35

    defaultConfig {
        applicationId = "edu.ucsd.cse110.secards.habitizer_test"
        minSdk = 31
>>>>>>> f710fb1 (Merge)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
<<<<<<< HEAD
        
        // Enable Room schema export function
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
=======
>>>>>>> f710fb1 (Merge)
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
<<<<<<< HEAD
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

=======
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
>>>>>>> f710fb1 (Merge)
}

dependencies {

<<<<<<< HEAD
    implementation(libs.android.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":lib"))
    implementation(project(":observables"))
    implementation(project(":observables"))

    // ThreeTen Android Backport for handling Java 8 date/time
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")
    
    // Room database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    // Optional - Kotlin extensions and coroutines support
    implementation("androidx.room:room-ktx:$roomVersion")
    // Optional - Testing support
    testImplementation("androidx.room:room-testing:$roomVersion")

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
=======
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
>>>>>>> f710fb1 (Merge)
