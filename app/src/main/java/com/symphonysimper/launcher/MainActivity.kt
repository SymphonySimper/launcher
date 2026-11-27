package com.symphonysimper.launcher

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import com.symphonysimper.launcher.ui.Drawer
import com.symphonysimper.launcher.ui.Home
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
    var drawerOpen by remember { mutableStateOf(false) }
    var focusSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    fun close() {
        focusManager.clearFocus()
        drawerOpen = false
        query = ""
    }
    LaunchedEffect(resetSignal) { close() }
    BackHandler(drawerOpen) { close() }

    val visible = remember(repo.all, prefs.hidden) { repo.all.filter { it.key !in prefs.hidden } }
    val dock = remember(repo.all, prefs.dock) { prefs.dock.mapNotNull(repo::find) }
    val spatial = MaterialTheme.motionScheme

    Box(Modifier.fillMaxSize()) {
        Home(
            dock = dock,
            onOpenDrawer = { focusSearch = false; drawerOpen = true },
            onSearch = { focusSearch = true; drawerOpen = true },
            onLauncherSettings = { context.start(Intent(context, SettingsActivity::class.java)) },
            onSystemSettings = { context.openSystemSettings() },
        )
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(spatial.fastSpatialSpec()) { it / 4 } + fadeIn(spatial.fastEffectsSpec()),
            exit = slideOutVertically(spatial.fastSpatialSpec()) { it / 4 } + fadeOut(spatial.fastEffectsSpec()),
        ) {
            Drawer(
                apps = visible,
                query = query,
                onQueryChange = { query = it },
                focusSearch = focusSearch,
                onClose = ::close,
            )
        }
    }
}
