package com.symphonysimper.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.symphonysimper.launcher.ui.theme.Sizes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.concurrent.ConcurrentHashMap

class App(
    /** Stable id used in settings and backups: "package/activity". */
    val key: String,
    val component: ComponentName,
    val user: UserHandle,
    val label: String,
    val icon: ImageBitmap,
) {
    val pkg: String get() = component.packageName
}

/** Apps hidden on first run. They exist on every Android phone. */
private val DEFAULT_HIDDEN = setOf("com.android.vending", "com.android.settings", "com.android.stk")

/** Loads launchable apps and their icons off the main thread and keeps the list current. */
class AppRepository(private val context: Context, private val prefs: Prefs) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val icons = ConcurrentHashMap<String, ImageBitmap>()
    /** Themed icons' monochrome layers, only for the apps that show them. Changed on the main thread. */
    private val monochromes = mutableStateMapOf<String, ImageBitmap>()
    /** Apps whose icon has no monochrome layer, so it isn't looked for again. Main thread only. */
    private val noMonochrome = HashSet<String>()
    private var loadJob: Job? = null

    /** Every launchable app, hidden ones included, sorted by label. */
    var all by mutableStateOf<List<App>>(emptyList())
        private set

    fun start() {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
        // When the dock or the themed icon settings change, load or free just the icons that changed.
        scope.launch(Dispatchers.Main) {
            snapshotFlow { themedKeys() }.drop(1).collectLatest { syncMonochromes(it, all) }
        }
    }

    /**
     * The icon's monochrome layer as a white mask, for a themed icon, or null if the app has none or
     * it isn't shown themed. Reading it in composition updates just that icon when it loads.
     */
    fun monochrome(key: String): ImageBitmap? = monochromes[key]

    /** Keys of the apps shown with themed icons; null means all of them. */
    private fun themedKeys(): Set<String>? = when {
        prefs.themedDrawer -> null
        prefs.themedDock -> prefs.dock.toSet()
        else -> emptySet()
    }

    fun find(key: String): App? = all.firstOrNull { it.key == key }

    private fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch {
            val list = load()
            withContext(Dispatchers.Main) {
                // Themed icons first, so the dock never shows the full-colour icons for a frame.
                syncMonochromes(themedKeys(), list)
                all = list
                if (!prefs.initialized) applyDefaults(list)
            }
        }
    }

    private val density get() = context.resources.displayMetrics.densityDpi
    private val iconPx get() = (Sizes.AppIcon.value * context.resources.displayMetrics.density).toInt()

    private fun load(): List<App> {
        val density = density
        val sizePx = iconPx
        val collator = Collator.getInstance()
        return launcherApps.getActivityList(null, Process.myUserHandle())
            .map { info ->
                val key = info.componentName.flattenToString()
                App(
                    key = key,
                    component = info.componentName,
                    user = info.user,
                    label = info.label.toString(),
                    icon = icons.getOrPut(key) { IconRenderer.render(info.getIcon(density), sizePx) },
                )
            }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /**
     * Makes [monochromes] hold exactly the themed icons shown: frees the rest and renders the missing
     * ones off the main thread. [themed] is the keys shown themed, or null for every app.
     */
    private suspend fun syncMonochromes(themed: Set<String>?, apps: List<App>) {
        val wanted = if (themed == null) apps else apps.filter { it.key in themed }
        val keep = wanted.mapTo(HashSet()) { it.key }
        (monochromes.keys - keep).forEach { monochromes.remove(it) }
        val missing = wanted.filter { it.key !in monochromes && it.key !in noMonochrome }
        if (missing.isEmpty()) return
        val rendered = withContext(Dispatchers.Default) {
            val density = density
            val sizePx = iconPx
            missing.map { app ->
                val info = launcherApps.resolveActivity(Intent(Intent.ACTION_MAIN).setComponent(app.component), app.user)
                app.key to info?.getIcon(density)?.let { IconRenderer.renderMonochrome(it, sizePx) }
            }
        }
        rendered.forEach { (key, bitmap) -> if (bitmap != null) monochromes[key] = bitmap else noMonochrome += key }
    }

    private fun applyDefaults(list: List<App>) {
        val hide = DEFAULT_HIDDEN + context.packageName
        prefs.hidden = list.filter { it.pkg in hide }.map { it.key }.toSet()
        prefs.dock = defaultDockPackages().mapNotNull { pkg -> list.firstOrNull { it.pkg == pkg }?.key }
            .distinct().take(MAX_DOCK)
        prefs.initialized = true
    }

    /** Phone, messages and camera: whichever apps are the defaults on this phone. */
    private fun defaultDockPackages(): List<String> {
        val camera = context.packageManager
            .resolveActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        return listOfNotNull(
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage,
            Telephony.Sms.getDefaultSmsPackage(context),
            camera,
        )
    }

    private fun changed(packageName: String) {
        icons.keys.removeAll { it.startsWith("$packageName/") }
        monochromes.keys.filter { it.startsWith("$packageName/") }.forEach { monochromes.remove(it) }
        noMonochrome.removeAll { it.startsWith("$packageName/") }
        reload()
    }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = reload()
    }
}

/** Draws every icon as a squircle. Old non-adaptive icons are shrunk onto a white squircle. */
object IconRenderer {
    fun squircle(size: Float) = Path().apply {
        moveTo(.5f * size, 0f)
        cubicTo(.92f * size, 0f, size, .08f * size, size, .5f * size)
        cubicTo(size, .92f * size, .92f * size, size, .5f * size, size)
        cubicTo(.08f * size, size, 0f, .92f * size, 0f, .5f * size)
        cubicTo(0f, .08f * size, .08f * size, 0f, .5f * size, 0f)
        close()
    }

    fun render(icon: Drawable, size: Int): ImageBitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        if (icon is AdaptiveIconDrawable) {
            // The layers are drawn 50% larger than the bounds; the mask below crops them.
            icon.setBounds(0, 0, size, size)
            icon.background?.draw(canvas)
            icon.foreground?.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
            val inset = (size * LEGACY_INSET).toInt()
            icon.setBounds(inset, inset, size - inset, size - inset)
            icon.draw(canvas)
        }
        // Clear everything outside the squircle, anti-aliased.
        val outside = squircle(size.toFloat()).apply { fillType = Path.FillType.INVERSE_WINDING }
        canvas.drawPath(outside, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })
        return bitmap.toGpu()
    }

    /**
     * The icon's monochrome layer as a white mask on transparent, placed like the foreground layer, to be
     * tinted when drawn. Null if the app doesn't provide one.
     */
    fun renderMonochrome(icon: Drawable, size: Int): ImageBitmap? {
        val mono = (icon as? AdaptiveIconDrawable)?.monochrome ?: return null
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        // Layers are 108dp with the middle 72dp showing: a quarter of the icon's size sticks out each side.
        val outset = size / 4
        mono.setBounds(-outset, -outset, size + outset, size + outset)
        mono.draw(canvas)
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN)
        return bitmap.toGpu()
    }

    // Keep only the GPU copy. A normal bitmap lives in app memory and also gets copied to the GPU
    // to be drawn, so every icon would be held twice.
    private fun Bitmap.toGpu(): ImageBitmap {
        val hardware = copy(Bitmap.Config.HARDWARE, false) ?: return asImageBitmap().also { prepareToDraw() }
        recycle()
        return hardware.asImageBitmap()
    }

    private const val LEGACY_INSET = 0.17f
}

