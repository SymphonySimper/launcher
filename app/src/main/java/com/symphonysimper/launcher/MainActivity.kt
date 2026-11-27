package com.symphonysimper.launcher

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
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
import androidx.compose.ui.graphics.CompositingStrategy
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
import com.symphonysimper.launcher.ui.rankApps
import com.symphonysimper.launcher.ui.rememberContacts
import com.symphonysimper.launcher.ui.rememberDrawerState
import com.symphonysimper.launcher.ui.searchBarBottom
import com.symphonysimper.launcher.ui.theme.LauncherTheme

class MainActivity : ComponentActivity() {
    /** Bumped whenever the launcher should go back to a clean home screen. */
    private var resetSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdge()
        setContent { LauncherTheme { LauncherRoot(resetSignal) } }
    }

    override fun onResume() {
        super.onResume()
        val prefs = LauncherApp.instance.prefs
        if (!prefs.tilePrompted) {
            prefs.tilePrompted = true
            requestAddTile()
        }
    }

    override fun onStop() {
        super.onStop()
        resetSignal++
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) resetSignal++
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        edgeToEdge() // dark mode may have changed; status bar icons follow it
    }

    private fun edgeToEdge() = enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
    )
}

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
    // However the drawer opens or closes (tap, swipe, back, reset), focus and the query follow it.
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

    Box(Modifier.fillMaxSize().onSizeChanged { drawer.range = it.height / 4f }) {
        Home(
            dock = dock,
            drawer = drawer,
            // A fresh task each time, so settings opens on its main page rather than where it was left.
            onLauncherSettings = {
                context.start(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
            },
            onSystemSettings = { context.openSystemSettings() },
            modifier = Modifier.graphicsLayer {
                alpha = 1f - drawer.progress.coerceIn(0f, 1f)
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
        )
        // The drawer stays composed so opening it is just a redraw; progress is read only while
        // drawing, so following the finger never recomposes. The search bar below stays put on top.
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val p = drawer.progress
                alpha = p.coerceIn(0f, 1f)
                // Fade each draw rather than an offscreen copy of the whole drawer: far less GPU work.
                compositingStrategy = CompositingStrategy.ModulateAlpha
                // Closed, it also moves off screen, so it can't catch touches meant for home. It stops
                // at the top even when the spring overshoots, or wallpaper would show under the search bar.
                translationY = if (p <= 0f) size.height else (1f - p.coerceAtMost(1f)) * drawer.range
            },
        ) {
            Drawer(apps = visible, query = query, appResults = appResults, contacts = contacts, drawer = drawer)
        }
        SearchField(
            value = query,
            onValueChange = { query = it },
            onSearch = ::onEnter,
            focusRequester = searchFocus,
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))
                .padding(start = 16.dp, end = 16.dp, bottom = searchBarBottom())
                .fillMaxWidth()
                // On home the bar is a button: a tap opens the drawer and then focuses it. It can't take
                // focus while the drawer is closed, so Android restoring focus can't reopen the drawer.
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
