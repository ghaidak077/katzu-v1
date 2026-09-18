plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.katzu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.katzu.grmapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val envFile = rootProject.file(".env")
        var workerBaseUrl = "https://katzu-auth-worker.ghaidakalosh008.workers.dev"
        var aiServerUrl = "$workerBaseUrl/ai"

        if (envFile.exists()) {
            envFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("WORKER_BASE_URL=")) {
                    val value = trimmed.substringAfter("WORKER_BASE_URL=").trim().removeSuffix("/")
                    if (value.isNotEmpty()) {
                        workerBaseUrl = value
                    }
                }
                if (trimmed.startsWith("AI_SERVER_URL=")) {
                    val value = trimmed.substringAfter("AI_SERVER_URL=").trim()
                    if (value.isNotEmpty()) {
                        aiServerUrl = value
                    }
                }
            }
        }

        // Zero secrets in app APK: All Gemini API keys live exclusively in Cloudflare Worker secrets
        buildConfigField("String", "GEMINI_API_KEY", "\"\"")
        buildConfigField("String", "GEMINI_API_KEYS", "\"\"")
        buildConfigField("String", "WORKER_BASE_URL", "\"$workerBaseUrl\"")
        buildConfigField("String", "AI_SERVER_URL", "\"$aiServerUrl\"")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.posthog)
    implementation(libs.play.billing)

    testImplementation(libs.junit)
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
}
