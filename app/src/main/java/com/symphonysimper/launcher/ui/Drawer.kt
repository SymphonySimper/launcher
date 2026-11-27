package com.symphonysimper.launcher.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.symphonysimper.launcher.App
import com.symphonysimper.launcher.Contact
import com.symphonysimper.launcher.ENGINES
import com.symphonysimper.launcher.LauncherApp
import com.symphonysimper.launcher.appInfo
import com.symphonysimper.launcher.call
import com.symphonysimper.launcher.launch
import com.symphonysimper.launcher.openContact
import com.symphonysimper.launcher.playStoreSearch
import com.symphonysimper.launcher.searchContacts
import com.symphonysimper.launcher.ui.theme.pageBackground
import com.symphonysimper.launcher.webSearch
import com.symphonysimper.launcher.whatsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val COLUMNS = 4

/** App drawer. When there's a query it shows search results instead of the grid. */
@Composable
fun Drawer(
    apps: List<App>,
    query: String,
    onQueryChange: (String) -> Unit,
    focusSearch: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = LauncherApp.instance.prefs
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusSearch) { if (focusSearch) focus.requestFocus() }

    val appResults = remember(apps, query) { rankApps(apps, query) }
    var contacts by remember { mutableStateOf(emptyList<Contact>()) }
    LaunchedEffect(query, prefs.contacts) {
        if (query.isBlank() || !prefs.contacts) {
            contacts = emptyList()
            return@LaunchedEffect
        }
        delay(60) // let fast typing settle before hitting the contacts provider
        contacts = withContext(Dispatchers.IO) { context.searchContacts(query) }
    }

    fun onEnter() {
        when {
            query.isBlank() -> Unit
            appResults.isNotEmpty() -> context.launch(appResults.first())
            contacts.isNotEmpty() -> context.openContact(contacts.first())
            else -> context.webSearch(query)
        }
    }

    Surface(color = MaterialTheme.colorScheme.pageBackground, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (query.isBlank()) AppGrid(apps, onClose)
                else Results(query, appResults, contacts)
            }
            // Extra room above the gesture bar, not needed when the keyboard is up.
            val keyboard = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search apps, contacts, web",
                focusRequester = focus,
                onSearch = ::onEnter,
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = if (keyboard) 10.dp else 24.dp),
            )
        }
    }
}

@Composable
private fun AppGrid(apps: List<App>, onClose: () -> Unit) {
    val close by rememberUpdatedState(onClose)
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    // Pulling down while already at the top closes the drawer.
    val pullToClose = remember {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > threshold) {
                        pulled = 0f
                        close()
                    }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        modifier = Modifier.fillMaxSize().nestedScroll(pullToClose),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(apps, key = { it.key }) { AppCell(it) }
    }
}

/** Results sit at the bottom: web search on top, then contacts, then apps nearest the thumb. */
@Composable
private fun Results(query: String, apps: List<App>, contacts: List<Contact>) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState(), reverseScrolling = true)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WebRow(query, highlight = apps.isEmpty() && contacts.isEmpty())
            if (contacts.isNotEmpty()) {
                val whatsApp = rememberAppIcon("com.whatsapp")
                val phone = rememberAppIcon(remember { context.getSystemService(TelecomManager::class.java).defaultDialerPackage })
                Column(verticalArrangement = Arrangement.spacedBy(SplitGap)) {
                    contacts.forEachIndexed { i, contact ->
                        ContactRow(
                            contact = contact,
                            whatsAppIcon = whatsApp,
                            phoneIcon = phone,
                            highlight = apps.isEmpty() && i == 0,
                            top = i == 0,
                            bottom = i == contacts.lastIndex,
                            onOpen = { context.openContact(contact) },
                            onCall = { context.call(contact) },
                            onWhatsApp = { context.whatsApp(contact) },
                        )
                    }
                }
            }
            apps.chunked(COLUMNS).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { AppCell(it, Modifier.weight(1f), highlight = it === apps.first()) }
                    repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** The installed app's own icon (e.g. the real WhatsApp logo), or null if it isn't installed. */
@Composable
private fun rememberAppIcon(pkg: String?): ImageBitmap? {
    val all = LauncherApp.instance.apps.all
    return remember(all, pkg) { all.firstOrNull { it.pkg == pkg }?.icon }
}

/** One piece of a split row. */
@Composable
private fun Segment(
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(if (highlight) colors.secondaryContainer else colors.surfaceBright)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** Icon-only end piece of a split row (Play Store, WhatsApp, Call). */
@Composable
private fun IconSegment(shape: Shape, highlight: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .width(64.dp)
            .fillMaxSize()
            .clip(shape)
            .background(if (highlight) colors.secondaryContainer else colors.surfaceBright)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Play Store search on the left, web search with the default browser's icon on the right. */
@Composable
private fun WebRow(query: String, highlight: Boolean) {
    val context = LocalContext.current
    val prefs = LauncherApp.instance.prefs
    val browserPkg = remember {
        context.packageManager
            .resolveActivity(Intent(Intent.ACTION_VIEW, "https://example.com".toUri()), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
    }
    val browser = rememberAppIcon(browserPkg)
    val store = rememberAppIcon("com.android.vending")
    val playStore = prefs.playStore
    Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(SplitGap)) {
        if (playStore) {
            IconSegment(splitShape(end = false), highlight, onClick = { context.playStoreSearch(query) }) {
                if (store != null) Image(store, "Search Play Store", Modifier.size(30.dp))
                else Icon(Ic.play, "Search Play Store")
            }
        }
        Segment(splitShape(start = !playStore), onClick = { context.webSearch(query) }, Modifier.weight(1f), highlight) {
            if (browser != null) Image(browser, null, Modifier.size(30.dp))
            else Icon(Ic.globe, null, tint = MaterialTheme.colorScheme.onSurface)
            Text(
                ENGINES[prefs.engine]?.first?.let { "Search $it" } ?: "Search the web",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AppCell(app: App, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (highlight) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .combinedClickable(onClick = { context.launch(app) }, onLongClick = { menu = true })
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(app, 56.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                app.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (menu) {
            LauncherMenu(
                listOf(
                    MenuEntry(Ic.hide, "Hide") {
                        val prefs = LauncherApp.instance.prefs
                        prefs.hidden = prefs.hidden + app.key
                    },
                    MenuEntry(Ic.info, "App info") { context.appInfo(app) },
                ),
                arrow = true,
                onDismiss = { menu = false },
            )
        }
    }
}

/** Contact | WhatsApp | Call. Call is always the end piece, so it stays put for muscle memory. */
@Composable
private fun ContactRow(
    contact: Contact,
    whatsAppIcon: ImageBitmap?,
    phoneIcon: ImageBitmap?,
    highlight: Boolean,
    top: Boolean,
    bottom: Boolean,
    onOpen: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val hasWhatsApp = contact.whatsAppDataId != null
    val hasCall = contact.phone != null
    Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(SplitGap)) {
        Segment(splitShape(top, bottom, end = !hasWhatsApp && !hasCall), onOpen, Modifier.weight(1f), highlight) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(40)).background(if (highlight) colors.surfaceBright else colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(contact.name.take(1).uppercase(), color = colors.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
            }
            Text(contact.name, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (hasWhatsApp) {
            IconSegment(splitShape(top, bottom, start = false, end = !hasCall), highlight, onWhatsApp) {
                if (whatsAppIcon != null) Image(whatsAppIcon, "WhatsApp", Modifier.size(32.dp))
                else Icon(Ic.chat, "WhatsApp", tint = colors.onSurface)
            }
        }
        if (hasCall) {
            IconSegment(splitShape(top, bottom, start = false), highlight, onCall) {
                if (phoneIcon != null) Image(phoneIcon, "Call", Modifier.size(32.dp))
                else Icon(Ic.phone, "Call", tint = colors.onSurface)
            }
        }
    }
}

/** Name starts with the query first, then a word starts with it, then it appears anywhere. */
fun rankApps(apps: List<App>, query: String): List<App> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return apps.mapNotNull { app ->
        val label = app.label.lowercase()
        val at = label.indexOf(q)
        when {
            at == 0 -> app to 0
            at > 0 && !label[at - 1].isLetterOrDigit() -> app to 1
            at > 0 -> app to 2
            else -> null
        }
    }.sortedBy { it.second }.map { it.first }.take(COLUMNS * 2)
}
