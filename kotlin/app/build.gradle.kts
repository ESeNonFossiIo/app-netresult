plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "org.comichao.netresult"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.comichao.netresult"
        minSdk = 24
        targetSdk = 36
        // Kivy build shipped versionCode 2 / 0.0.4; keep strictly increasing.
        versionCode = 7
        versionName = "0.2.3"
    }

    signingConfigs {
        // Optional: keep the debug key next to the release key. Same defaults as Android's own.
        System.getenv("DEBUG_KEYSTORE")?.let { path -> getByName("debug") { storeFile = file(path) } }
        create("release") {
            storeFile = System.getenv("KEYSTORE")?.let { file(it) }
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS") ?: "netresult"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            // -PidSuffix=.shots installs a throwaway copy with its own empty database (README screenshots).
            (project.findProperty("idSuffix") as String?)?.let { applicationIdSuffix = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    androidResources { generateLocaleConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

// APKs are named netresult-<versionName>-<variant>.apk
base.archivesName.set(provider { "netresult-${android.defaultConfig.versionName}" })

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation(project(":scoring"))
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")  // real org.json; the android.jar one is stubbed in unit tests
}
