plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.DefaultCompany.Tahmin11"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.DefaultCompany.Tahmin11"
        minSdk = 24
        targetSdk = 35
        versionCode = 116
        versionName = "116"
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

    lint {
        checkReleaseBuilds = false
        abortOnError = false
        baseline = file("lint-baseline.xml")
        disable.add("MobileAdsSdkOutdatedVersion")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // --- Yandex Mobile Ads ---
    implementation("com.yandex.android:mobileads:7.18.1")

    // --- AdMob (Google Mobile Ads SDK) ---
    implementation("com.google.android.gms:play-services-ads:24.0.0")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.webkit:webkit:1.12.1")

    // Ads Identifier (kalsın)
    implementation("com.google.android.gms:play-services-ads-identifier:18.1.0")
}
