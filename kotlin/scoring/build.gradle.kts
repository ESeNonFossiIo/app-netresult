plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// Scoring rules and the watch-to-phone session payload, shared by :app and :wear.
android {
    namespace = "org.comichao.netresult.scoring"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    api("com.google.android.gms:play-services-wearable:18.2.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
