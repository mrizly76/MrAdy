plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.sitephotos.app"
    compileSdk = 35

    defaultConfig {
        // This ID + the SHA-1 of keystore/sitephotos.jks are what you register in Google Cloud.
        applicationId = "com.sitephotos.p219220"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // One fixed signing key for debug AND release, so the SHA-1 registered
    // with Google never changes, whichever machine builds the APK.
    signingConfigs {
        create("shared") {
            storeFile = file("keystore/sitephotos.jks")
            storePassword = "sitephotos"
            keyAlias = "sitephotos"
            keyPassword = "sitephotos"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // In-app camera (photos never go to the phone gallery)
    val camerax = "1.4.1"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Google account authorization for Drive
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // HTTP calls to the Google Drive REST API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
