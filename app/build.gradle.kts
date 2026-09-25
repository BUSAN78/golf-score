import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use(::load)
}

android {
    namespace = "jp.example.golfscore"
    compileSdk = 36

    defaultConfig {
        applicationId = "jp.example.golfscore"
        minSdk = 26
        targetSdk = 36
        versionCode = 21
        versionName = "1.13.1"
    }

    buildFeatures {
        compose = true
        resValues = true
    }

    defaultConfig {
        resValue("string", "rakuten_application_id", localProperties.getProperty("rakuten.applicationId", ""))
        resValue("string", "rakuten_access_key", localProperties.getProperty("rakuten.accessKey", ""))
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
}
