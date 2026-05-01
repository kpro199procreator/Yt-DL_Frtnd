import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    id("com.chaquo.python") version "16.1.0"
}

android {
    namespace  = "com.ytmusicdl.app"
    compileSdk = 35

    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        applicationId = "com.ytmusicdl.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "0.1.0-alpha"

        // Necesario para NewPipe Extractor con minSdk < 33
        multiDexEnabled = true
    }

    // Desugaring para que NewPipe Extractor funcione en Android < 13
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }

    // Filtrar arquitecturas que no necesitamos (reduce tamaño APK)
    // ffmpeg-kit incluye binarios nativos para 4 ABIs
    packaging {
        jniLibs {
            // Excluir duplicados de dependencias nativas
            excludes += setOf("META-INF/DEPENDENCIES")
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
        }
    }

    // Keystore
    val keystoreFile = rootProject.file("app/keystore.jks")
    if (keystoreFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile     = keystoreFile
                storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: localProp("storePassword")
                keyAlias      = System.getenv("SIGNING_KEY_ALIAS")      ?: localProp("keyAlias")
                keyPassword   = System.getenv("SIGNING_KEY_PASSWORD")   ?: localProp("keyPassword")
            }
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }
}

fun localProp(key: String): String {
    val f = rootProject.file("keystore.properties")
    if (!f.exists()) return ""
    return Properties().also { it.load(f.inputStream()) }.getProperty(key, "")
}

dependencies {
    // Desugaring — necesario para NewPipe con minSdk 26
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.2")

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.ext)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.ktx)
    implementation(libs.core.ktx)
    debugImplementation(libs.compose.ui.tooling)

    // Imágenes
    implementation(libs.coil.compose)

    // HTTP (para lrclib y otras APIs REST)
    implementation(libs.okhttp)

    // Coroutines
    implementation(libs.coroutines.android)

    // Room — historial de descargas
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // ExoPlayer — reproducción de preview
    implementation(libs.media3.exoplayer)

    // NewPipe Extractor — búsqueda y extracción de streams YT Music
    // sin necesidad de API key ni Python
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.2")

    // ffmpeg-kit (fork community — el original fue archivado abril 2025)
    // Solo el paquete de audio para menor tamaño
    implementation("com.github.pgahq:ffmpeg-kit-fork:audio-6.0-2")

    // JAudioTagger — escritura de tags ID3/MP4 en Kotlin/Java puro
    implementation("net.jthink:jaudiotagger:3.0.1")
}


chaquopy {
    defaultConfig {
        pip {
            install("yt-dlp")
            install("ytmusicapi")
            install("requests")
        }
    }
}
