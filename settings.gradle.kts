pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://chaquo.com/maven")
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // JitPack para NewPipe Extractor
        maven("https://jitpack.io")
    }
}
rootProject.name = "ytmusicdl"
include(":app")
