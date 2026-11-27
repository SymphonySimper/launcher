package com.symphonysimper.launcher.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which code the launcher runs at startup and while it's used, so it's compiled ahead of
 * time instead of interpreted on first use. Regenerate after big changes:
 *
 *   ./gradlew :app:generateBaselineProfile -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
 *
 * The flag stops Gradle uninstalling the launcher afterwards, which would wipe its settings. Running it
 * restarts the launcher many times; on some phones that resets the live wallpaper and the default home
 * app, so set them again afterwards.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        useLauncher()
    }
}
