import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.baselineprofile)
}

fun git(vararg args: String): String? = runCatching {
    providers.exec { commandLine("git", *args) }.standardOutput.asText.get().trim()
}.getOrNull()

val commitCount: Int = git("rev-list", "--count", "HEAD")?.toInt()?.also {
    check(git("rev-parse", "--is-shallow-repository") != "true") {
        "The version code is the commit count, which a shallow clone gets wrong. Run git fetch --unshallow."
    }
} ?: 1

val commitDate: String = git("log", "-1", "--format=%ct")?.let {
    Instant.ofEpochSecond(it.toLong()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
} ?: "0000.00.00"

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
        versionCode = commitCount
        versionName = "$commitDate.$commitCount"
    }

    signingConfigs {
        fun signing(name: String) = providers.environmentVariable(name).orElse(providers.gradleProperty(name))
        val keystore = signing("SIGNING_KEYSTORE").orNull
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = signing("SIGNING_STORE_PASSWORD").get()
                keyAlias = signing("SIGNING_KEY_ALIAS").get()
                keyPassword = signing("SIGNING_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
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
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
}
