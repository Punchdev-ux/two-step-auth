plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.two_step_auth"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.two_step_auth"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// All dependencies go inside this single block
dependencies {
    // Standard Android and Test dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the BoM for the Firebase platform
    implementation(platform(libs.firebase.bom))

    // Firebase Dependencies (no versions needed due to BoM)
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-firestore-ktx") // Added Firestore

    // TOTP and QR Code Dependencies using the version catalog
    implementation(libs.totp)
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
}