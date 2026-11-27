package com.symphonysimper.launcher

import android.Manifest
import android.app.ActivityOptions
import android.window.SplashScreen
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.app.StatusBarManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import android.net.Uri
import java.net.URLEncoder

val ENGINES = linkedMapOf(
    "google" to ("Google" to "https://www.google.com/search?q=%s"),
    "brave" to ("Brave" to "https://search.brave.com/search?q=%s"),
    "duckduckgo" to ("DuckDuckGo" to "https://duckduckgo.com/?q=%s"),
)

private const val WHATSAPP = "com.whatsapp"
const val WHATSAPP_MIME = "vnd.android.cursor.item/vnd.com.whatsapp.profile"

fun Context.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

fun Context.start(intent: Intent, options: Bundle? = null) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options)
    } catch (_: ActivityNotFoundException) {
        toast("No app can open this")
    } catch (_: SecurityException) {
        toast("Not allowed")
    }
}

fun Context.startOwn(intent: Intent) =
    start(intent, ActivityOptions.makeBasic().setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_SOLID_COLOR).toBundle())

fun Context.launch(app: App, view: View? = null, bounds: Rect? = null) {
    var options: Bundle? = null
    var source: Rect? = null
    if (view != null && bounds != null) {
        options = ActivityOptions.makeClipRevealAnimation(view, bounds.left, bounds.top, bounds.width(), bounds.height()).toBundle()
        val origin = IntArray(2).also(view::getLocationOnScreen)
        source = Rect(bounds).apply { offset(origin[0], origin[1]) }
    }
    try {
        getSystemService(LauncherApps::class.java).startMainActivity(app.component, app.user, source, options)
    } catch (_: Exception) {
        toast("Can't open ${app.label}")
    }
}

fun Context.open(intent: Intent, fallback: Intent?, view: View?, bounds: Rect?) {
    val options = if (view != null && bounds != null) {
        ActivityOptions.makeClipRevealAnimation(view, bounds.left, bounds.top, bounds.width(), bounds.height()).toBundle()
    } else null
    for (candidate in listOfNotNull(intent, fallback)) {
        try {
            startActivity(candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options)
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }
    toast("No app can open this")
}

fun clockIntent() = Intent(AlarmClock.ACTION_SHOW_ALARMS)

fun calendarIntents(now: Long) =
    Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(now.toString()).build()) to
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)

fun Context.appInfo(app: App) =
    getSystemService(LauncherApps::class.java).startAppDetailsActivity(app.component, app.user, null, null)

private fun encode(query: String) = URLEncoder.encode(query.trim(), "UTF-8")

fun Context.webSearch(query: String) {
    val prefs = LauncherApp.instance.prefs
    val template = if (prefs.engine == "custom" && prefs.customUrl.isNotBlank()) prefs.customUrl
    else (ENGINES[prefs.engine] ?: ENGINES.getValue("google")).second
    val url = if ("%s" in template) template.replace("%s", encode(query)) else template + encode(query)
    start(Intent(Intent.ACTION_VIEW, url.toUri()))
}

fun Context.playStoreSearch(query: String) {
    val market = Intent(Intent.ACTION_VIEW, "market://search?q=${encode(query)}".toUri()).setPackage("com.android.vending")
    if (market.resolveActivity(packageManager) != null) start(market)
    else start(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/search?q=${encode(query)}".toUri()))
}

fun Context.openContact(contact: Contact) =
    start(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)))

fun Context.call(contact: Contact) {
    val number = contact.phone ?: return
    val canCall = checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    start(Intent(if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL, "tel:${Uri.encode(number)}".toUri()))
}

fun Context.whatsApp(contact: Contact) {
    val dataId = contact.whatsAppDataId ?: return
    start(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId), WHATSAPP_MIME)
            .setPackage(WHATSAPP),
    )
}

fun Context.openSystemSettings() = start(Intent(Settings.ACTION_SETTINGS))

/**
 * Pulls down the notification shade. There's no public API for this, so it calls the hidden
 * StatusBarManager.expandNotificationsPanel(), the way other launchers do; it needs EXPAND_STATUS_BAR.
 */
fun Context.expandNotifications() {
    runCatching {
        val manager = getSystemService(StatusBarManager::class.java)
        StatusBarManager::class.java.getMethod("expandNotificationsPanel").invoke(manager)
    }
}

fun ComponentActivity.followPortraitOnly() {
    val prefs = LauncherApp.instance.prefs
    lifecycleScope.launch {
        snapshotFlow { prefs.portraitOnly }.collect { portrait ->
            requestedOrientation = if (portrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
