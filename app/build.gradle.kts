plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.baselineprofile)
}

fun git(vararg args: String): String? = runCatching {
    providers.exec { commandLine("git", *args); environment("TZ", "UTC") }.standardOutput.asText.get().trim()
}.getOrNull()

/** Commits up to HEAD; 1 outside a git checkout. Needs full history (CI checks out with fetch-depth 0). */
val commitCount: Int = git("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1

/**
 * HEAD's commit date in UTC, so building the same commit twice gives the same version, and commits
 * made in different time zones (locally, or by GitHub when merging) never date backwards.
 */
val commitDate: String = git("log", "-1", "--format=%cd", "--date=format-local:%Y.%m.%d") ?: "0000.00.00"

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
        // Calendar versioning: the commit's date, then the commit count so same-day releases differ.
        versionName = "$commitDate.$commitCount"
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

androidComponents {
    // The baseline profile plugin copies release into this build type to record the profile from. It must
    // keep real class names, or the profile won't match anything in the release build. This runs after the
    // plugin has made the build type.
    finalizeDsl { android ->
        android.buildTypes.findByName("nonMinifiedRelease")?.optimization?.enable = false
    }
}

baselineProfile {
    // Generated on a phone with `./gradlew :app:generateBaselineProfile` (see baselineprofile/) and
    // committed under src/release/generated, so normal builds and CI never need a device.
    // Startup code is also packed into the first dex file, so less of it is read from disk at launch.
    dexLayoutOptimization = true
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Compiles the baseline profile on phones that don't get it from Play, which is all of ours.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    debugImplementation(libs.androidx.compose.ui.tooling)
}
