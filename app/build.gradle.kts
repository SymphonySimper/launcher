plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/** Commits up to HEAD; 1 outside a git checkout. Needs full history (CI checks out with fetch-depth 0). */
val commitCount: Int = runCatching {
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }.standardOutput.asText.get().trim().toInt()
}.getOrDefault(1)

android {
    namespace = "com.symphonysimper.launcher"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.symphonysimper.launcher"
        minSdk = 33
        targetSdk = 37
        // Every commit raises the version, so any newer build (local or released) installs over an older one.
        versionCode = commitCount
        versionName = "1.0.$commitCount"
    }

    signingConfigs {
        // The release workflow signs with the key in these variables (see .github/workflows/release.yml).
        val keystore = providers.environmentVariable("SIGNING_KEYSTORE").orNull
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            // Locally it's signed with the debug key so the fast, optimised build can be installed over a
            // debug one. Debug builds of Compose apps are much slower than what users get, so judge
            // smoothness here.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
