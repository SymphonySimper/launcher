package com.symphonysimper.launcher.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

const val PACKAGE = "com.symphonysimper.launcher"

fun MacrobenchmarkScope.useLauncher() {
    openAndScrollDrawer()
    search()
    device.swipe(device.displayWidth / 2, device.displayHeight / 3, device.displayWidth / 2, device.displayHeight / 3, 200)
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
}

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
    device.swipe(device.displayWidth / 8, device.displayHeight / 5, device.displayWidth / 8, device.displayHeight / 5, 200)
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
}

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
    device.pressBack()
    device.pressBack()
    device.waitForIdle()
}
