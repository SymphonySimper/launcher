package com.symphonysimper.launcher

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Base64
import java.io.FileDescriptor
import java.io.PrintWriter
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import com.symphonysimper.launcher.ui.Drawer
import com.symphonysimper.launcher.ui.Home
import com.symphonysimper.launcher.ui.SearchField
import com.symphonysimper.launcher.ui.WallpaperFade
import com.symphonysimper.launcher.ui.rankApps
import com.symphonysimper.launcher.ui.rememberContacts
import com.symphonysimper.launcher.ui.rememberDrawerState
import com.symphonysimper.launcher.ui.searchBarBottom
import com.symphonysimper.launcher.ui.theme.LauncherTheme

class MainActivity : ComponentActivity() {
    private var resetSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdge()
        followPortraitOnly()
        setContent { LauncherTheme { LauncherRoot(resetSignal) } }
    }

    override fun onStop() {
        super.onStop()
        resetSignal++
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) resetSignal++
    }

    /**
     * Backs up and restores settings from a computer, so testing that reinstalls the launcher can put
     * them back. Only adb and the system may call this (it needs the DUMP permission):
     *
     *   adb shell dumpsys activity com.symphonysimper.launcher/.MainActivity export-settings > backup.json
     *   adb shell dumpsys activity com.symphonysimper.launcher/.MainActivity import-settings $(base64 -w0 backup.json)
     */
    override fun dump(prefix: String, fd: FileDescriptor?, writer: PrintWriter, args: Array<out String>?) {
        val prefs = LauncherApp.instance.prefs
        when (args?.firstOrNull()) {
            "export-settings" -> writer.println(prefs.toJson())
            "import-settings" -> runCatching { prefs.fromJson(String(Base64.decode(args.getOrNull(1), Base64.DEFAULT))) }
                .onSuccess { writer.println("Settings restored") }
                .onFailure { writer.println("That isn't a launcher backup: ${it.message}") }
            else -> super.dump(prefix, fd, writer, args)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        edgeToEdge()
    }

    private fun edgeToEdge() = enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
    )
}

private val HomeLift = 40.dp
private const val HomeShrink = 0.04f

private const val ScrimAlpha = 0.28f

private val SheetCorner = 28.dp

@Composable
private fun LauncherRoot(resetSignal: Int) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = LauncherApp.instance.prefs
    val repo = LauncherApp.instance.apps
    val drawer = rememberDrawerState()
    var query by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }
    var focusOnOpen by remember { mutableStateOf(false) }

    LaunchedEffect(resetSignal) { drawer.snapTo(false) }
    BackHandler(drawer.open) { drawer.animateTo(false) }
    LaunchedEffect(drawer.open) {
        if (drawer.open) {
            if (focusOnOpen) searchFocus.requestFocus()
        } else {
            focusManager.clearFocus()
            query = ""
        }
        focusOnOpen = false
    }

    val visible = remember(repo.all, prefs.hidden) { repo.all.filter { it.key !in prefs.hidden } }
    val dock = remember(repo.all, prefs.dock) { prefs.dock.mapNotNull(repo::find) }
    val appResults = remember(visible, query) { rankApps(visible, query) }
    val contacts = rememberContacts(query)

    fun onEnter() {
        when {
            query.isBlank() -> Unit
            appResults.isNotEmpty() -> context.launch(appResults.first())
            contacts.isNotEmpty() -> context.openContact(contacts.first())
            else -> context.webSearch(query)
        }
    }

    Box(Modifier.fillMaxSize().onSizeChanged { drawer.range = it.height.toFloat() }) {
        WallpaperFade()
        Home(
            dock = dock,
            drawer = drawer,
            onLauncherSettings = {
                context.startWithPlainSplash(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
            },
            onSystemSettings = { context.openSystemSettings() },
            modifier = Modifier.graphicsLayer {
                val p = drawer.progress
                translationY = -HomeLift.toPx() * p
                scaleX = 1f - HomeShrink * p
                scaleY = scaleX
                // The open drawer covers home, and a layer at zero alpha isn't drawn at all.
                alpha = if (p < 1f) 1f else 0f
            },
        )
        Box(
            Modifier.fillMaxSize().drawBehind {
                val p = drawer.progress
                if (p > 0f && p < 1f) drawRect(Color.Black, alpha = ScrimAlpha * p)
            },
        )
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val p = drawer.progress
                translationY = (1f - p) * size.height
                // Round top corners that square off as the sheet reaches the top. All four corners are
                // rounded, which keeps the fast round-rect clip: the bottom two are below the screen
                // until the sheet is fully open, when the radius is 0.
                shape = RoundedCornerShape(SheetCorner.toPx() * (1f - p))
                clip = true
            },
        ) {
            Drawer(apps = visible, query = query, appResults = appResults, contacts = contacts, drawer = drawer)
        }
        SearchField(
            value = query,
            onValueChange = { query = it },
            onSearch = ::onEnter,
            focusRequester = searchFocus,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))
                .padding(start = 16.dp, end = 16.dp, bottom = searchBarBottom())
                .fillMaxWidth()
                .focusProperties { canFocus = drawer.open }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        if (!drawer.open) {
                            focusOnOpen = true
                            drawer.animateTo(true)
                        }
                    }
                },
        )
    }
}
