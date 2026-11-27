package com.symphonysimper.launcher

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

const val MAX_DOCK = 4

/** Every launcher setting. Backed by SharedPreferences, observable from Compose. */
class Prefs(context: Context) {
    val sp: SharedPreferences = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    var initialized by bool("initialized", false)
    var dock by stringList("dock")
    var hidden by stringSet("hidden")
    var badges by bool("badges", true)
    var contacts by bool("contacts", false)
    var playStore by bool("play_store", true)
    var engine by string("engine", "google")
    var customUrl by string("custom_url", "")
    var wallpaperImages by bool("wallpaper_images", false)
    var sameImage by bool("same_image", false)
    /** Bumped whenever a wallpaper image file changes, so the wallpaper redraws. */
    var wallpaperVersion by int("wallpaper_version", 0)
    var stayAwake by bool("stay_awake", false)
    var savedTimeout by int("saved_timeout", 60_000)
    /** Keeps the launcher upright even with auto-rotate on. */
    var portraitOnly by bool("portrait_only", true)
    /** Pixel-style themed icons, where the app provides one: on in the dock, off in the drawer. */
    var themedDock by bool("themed_dock", true)
    var themedDrawer by bool("themed_drawer", false)

    fun toJson(): String {
        val json = JSONObject()
            .put("version", 1)
            .put("dock", JSONArray(dock))
            .put("hiddenApps", JSONArray(hidden.sorted()))
            .put("notificationBadges", badges)
            .put("portraitOnly", portraitOnly)
            .put("themedIcons", JSONObject().put("dock", themedDock).put("drawer", themedDrawer))
            .put(
                "search",
                JSONObject()
                    .put("engine", engine)
                    .put("customUrl", customUrl)
                    .put("contacts", contacts)
                    .put("playStore", playStore),
            )
            .put(
                "wallpaper",
                JSONObject()
                    .put("source", if (wallpaperImages) "images" else "generated")
                    .put("sameImageForBoth", sameImage),
            )
        // org.json escapes "/" which makes component names hard to read.
        return json.toString(2).replace("\\/", "/")
    }

    /** Applies a backup. Throws if the text is not a backup made by [toJson]. */
    fun fromJson(text: String) {
        val json = JSONObject(text)
        require(json.optInt("version") == 1) { "Unknown backup version" }
        json.optJSONArray("dock")?.let { dock = it.strings().take(MAX_DOCK) }
        json.optJSONArray("hiddenApps")?.let { hidden = it.strings().toSet() }
        if (json.has("notificationBadges")) badges = json.getBoolean("notificationBadges")
        if (json.has("portraitOnly")) portraitOnly = json.getBoolean("portraitOnly")
        json.optJSONObject("themedIcons")?.let { t ->
            themedDock = t.optBoolean("dock", themedDock)
            themedDrawer = t.optBoolean("drawer", themedDrawer)
        }
        json.optJSONObject("search")?.let { s ->
            engine = s.optString("engine", engine)
            customUrl = s.optString("customUrl", customUrl)
            contacts = s.optBoolean("contacts", contacts)
            playStore = s.optBoolean("playStore", playStore)
        }
        json.optJSONObject("wallpaper")?.let { w ->
            wallpaperImages = w.optString("source") == "images"
            sameImage = w.optBoolean("sameImageForBoth", sameImage)
        }
    }

    private fun JSONArray.strings() = List(length()) { getString(it) }

    private fun bool(key: String, default: Boolean) =
        pref(key, sp.getBoolean(key, default)) { putBoolean(key, it) }

    private fun int(key: String, default: Int) =
        pref(key, sp.getInt(key, default)) { putInt(key, it) }

    private fun string(key: String, default: String) =
        pref(key, sp.getString(key, default) ?: default) { putString(key, it) }

    private fun stringSet(key: String) =
        pref(key, sp.getStringSet(key, null)?.toSet() ?: emptySet()) { putStringSet(key, it) }

    private fun stringList(key: String) =
        pref(key, sp.getString(key, null)?.let { JSONArray(it).strings() } ?: emptyList()) {
            putString(key, JSONArray(it).toString())
        }

    private fun <T> pref(
        key: String,
        initial: T,
        write: SharedPreferences.Editor.(T) -> Unit,
    ) = object : ReadWriteProperty<Any?, T> {
        val state = mutableStateOf(initial)
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            if (state.value == value) return
            state.value = value
            sp.edit { write(value) }
        }
    }
}
