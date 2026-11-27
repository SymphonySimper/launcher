package com.symphonysimper.launcher

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.app.StatusBarManager
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

fun Context.start(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        toast("No app can open this")
    } catch (_: SecurityException) {
        toast("Not allowed")
    }
}

fun Context.launch(app: App) {
    try {
        getSystemService(LauncherApps::class.java).startMainActivity(app.component, app.user, null, null)
    } catch (_: Exception) {
        toast("Can't open ${app.label}")
    }
}

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

fun Context.requestAddTile() {
    getSystemService(StatusBarManager::class.java)?.requestAddTileService(
        ComponentName(this, StayAwakeTile::class.java),
        getString(R.string.tile_label),
        Icon.createWithResource(this, R.drawable.ic_tile),
        mainExecutor,
    ) { }
}


