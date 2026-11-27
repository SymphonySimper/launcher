package com.symphonysimper.launcher.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

const val PACKAGE = "com.symphonysimper.launcher"

/**
 * What people do with the launcher most: start it, open the drawer, scroll it, search, and close it.
 * Driven with gestures rather than test tags, so the app carries nothing extra for this.
 */
fun MacrobenchmarkScope.useLauncher() {
    openAndScrollDrawer()
    search()
    // Back on home: a long press shows the launcher menu.
    device.swipe(device.displayWidth / 2, device.displayHeight / 3, device.displayWidth / 2, device.displayHeight / 3, 200)
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
}

/** Swipes the drawer open from home, flings the app grid both ways, and closes it with back. */
fun MacrobenchmarkScope.openAndScrollDrawer() {
    val x = device.displayWidth / 2
    device.swipe(x, device.displayHeight * 3 / 4, x, device.displayHeight / 6, 30)
    device.waitForIdle()
    repeat(2) {
        device.swipe(x, device.displayHeight * 3 / 4, x, device.displayHeight / 4, 8)
        device.waitForIdle()
    }
    repeat(2) {
        device.swipe(x, device.displayHeight / 4, x, device.displayHeight * 3 / 4, 8)
        device.waitForIdle()
    }
    // Long-press an app for its menu, then dismiss it.
    device.swipe(device.displayWidth / 8, device.displayHeight / 5, device.displayWidth / 8, device.displayHeight / 5, 200)
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
}

/** Taps the search bar on home, types a query letter by letter, and closes the drawer. */
fun MacrobenchmarkScope.search() {
    val field = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 2_000) ?: return
    field.click()
    device.waitForIdle()
    var query = ""
    for (letter in "cal") {
        query += letter
        device.findObject(By.clazz("android.widget.EditText"))?.text = query
        device.waitForIdle()
    }
    device.findObject(By.clazz("android.widget.EditText"))?.text = ""
    device.waitForIdle()
    device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
    device.waitForIdle()
    device.pressBack() // hides the keyboard
    device.pressBack() // closes the drawer
    device.waitForIdle()
}
