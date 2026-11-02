plugins {
    id("com.android.library")
}

// Scoring rules and the watch-to-phone session payload, shared by :app and :wear.
android {
    namespace = "org.comichao.netresult.scoring"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    api("com.google.android.gms:play-services-wearable:20.0.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
}
