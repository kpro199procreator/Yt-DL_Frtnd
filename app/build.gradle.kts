import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Leer keystore desde archivo local (desarrollo) o env vars (CI)
val keystoreFile = rootProject.file("app/keystore.jks")
val hasKeystore  = keystoreFile.exists()
        || System.getenv("KEYSTORE_BASE64") != null

android {
    namespace  = "com.ytmusicdl.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ytmusicdl.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "0.3.0"
    }

    // Signing config — usa env vars en CI, keystore.properties en local
    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile     = keystoreFile
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                    ?: localProp("storePassword")
                keyAlias      = System.getenv("SIGNING_KEY_ALIAS")
                    ?: localProp("keyAlias")
                keyPassword   = System.getenv("SIGNING_KEY_PASSWORD")
                    ?: localProp("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
            isDebuggable        = true
        }
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

fun localProp(key: String): String {
    val props = Properties()
    val file  = rootProject.file("keystore.properties")
    if (file.exists()) props.load(file.inputStream())
    return props.getProperty(key, "")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
