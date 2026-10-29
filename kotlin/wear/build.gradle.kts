plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Same version as the phone app, read from its build file so `make bump` keeps both in step.
val appBuild = file("../app/build.gradle.kts").readText()
val appVersionName = Regex("""versionName\s*=\s*"([^"]+)"""").find(appBuild)!!.groupValues[1]
val appVersionCode = Regex("""versionCode\s*=\s*(\d+)""").find(appBuild)!!.groupValues[1].toInt()

// Wear OS companion: scores a match (or records a warm-up) and sends it to the phone app.
// The application id must equal the phone app's so the Data Layer connects them.
android {
    namespace = "org.comichao.netresult.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.comichao.netresult"
        minSdk = 30
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        // Same key as the phone app: the Data Layer only links apps with the same id and signature.
        create("release") {
            storeFile = System.getenv("KEYSTORE")?.let { file(it) }
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS") ?: "netresult"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release { signingConfig = signingConfigs.getByName("release") }
        debug {
            // Must match the phone build's -PidSuffix, or the two apps will not see each other.
            (project.findProperty("idSuffix") as String?)?.let { applicationIdSuffix = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

base.archivesName.set(provider { "netresult-wear-${android.defaultConfig.versionName}" })

dependencies {
    implementation(project(":scoring"))

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.health:health-services-client:1.0.0")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
