pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Eğer build sırasında "repository" hatası alırsan
    // FAIL_ON_PROJECT_REPOS yerine PREFER_SETTINGS yapabilirsin.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Yandex kütüphanesi
        maven { url = uri("https://mobile-sdk.yandex.ru/maven") }
    }
}

// --- EKLENEN KISIM BURASI ---
rootProject.name = "Tahmin11"
include(":app")
// ----------------------------