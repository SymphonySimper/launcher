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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.symphonysimper.launcher.App
import com.symphonysimper.launcher.Contact
import com.symphonysimper.launcher.ENGINES
import com.symphonysimper.launcher.LauncherApp
import com.symphonysimper.launcher.appInfo
import com.symphonysimper.launcher.call
import com.symphonysimper.launcher.openContact
import com.symphonysimper.launcher.playStoreSearch
import com.symphonysimper.launcher.searchContacts
import com.symphonysimper.launcher.ui.theme.pageBackground
import com.symphonysimper.launcher.webSearch
import com.symphonysimper.launcher.whatsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import com.symphonysimper.launcher.ui.theme.Sizes

private const val COLUMNS = 4

/** App drawer. When there's a query it shows search results instead of the grid. The search bar itself lives in LauncherRoot. */
@Composable
fun Drawer(apps: List<App>, query: String, appResults: List<App>, contacts: List<Contact>, drawer: DrawerState) {
    Surface(color = MaterialTheme.colorScheme.pageBackground, modifier = Modifier.fillMaxSize()) {
        // Lists run to the bottom edge and scroll behind the floating search bar; this padding only
        // keeps their last items clear of it once scrolled to the end.
        val bottom = WindowInsets.systemBars.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding() + searchBarSpace()
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            if (query.isBlank()) AppGrid(apps, drawer, bottom)
            else Results(query, appResults, contacts, bottom)
        }
    }
}

/** Contacts matching [query], looked up off the main thread once typing settles. */
@Composable
fun rememberContacts(query: String): List<Contact> {
    val context = LocalContext.current
    val enabled = LauncherApp.instance.prefs.contacts
    var contacts by remember { mutableStateOf(emptyList<Contact>()) }
    LaunchedEffect(query, enabled) {
        if (query.isBlank() || !enabled) {
            contacts = emptyList()
            return@LaunchedEffect
        }
        delay(60) // let fast typing settle before hitting the contacts provider
        contacts = withContext(Dispatchers.IO) { context.searchContacts(query) }
    }
    return contacts
}

@Composable
private fun AppGrid(apps: List<App>, drawer: DrawerState, bottom: Dp) {
    val grid = rememberLazyGridState()
    // Each time the drawer finishes closing, the grid goes back to the top for next time.
    LaunchedEffect(drawer) {
        snapshotFlow { drawer.progress <= 0f }.filter { it }.collect { grid.scrollToItem(0) }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        state = grid,
        modifier = Modifier.fillMaxSize().nestedScroll(drawer.pullToClose),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 24.dp, bottom = bottom + 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(apps, key = { it.key }) { AppCell(it) }
    }
}

/** Results sit at the bottom: web search on top, then contacts, then apps nearest the thumb. */
@Composable
private fun Results(query: String, apps: List<App>, contacts: List<Contact>, bottom: Dp) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState(), reverseScrolling = true)
                .padding(start = 12.dp, end = 12.dp, bottom = bottom),
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

/** Look and tap target shared by every piece of a split row. */
@Composable
private fun Modifier.segment(shape: Shape, highlight: Boolean, onClick: () -> Unit): Modifier {
    val colors = MaterialTheme.colorScheme
    return fillMaxSize()
        .clip(shape)
        .background(if (highlight) colors.secondaryContainer else colors.surfaceBright)
        .clickable(onClick = onClick)
}

/** One piece of a split row. */
@Composable
private fun Segment(
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) = Row(
    modifier.segment(shape, highlight, onClick).padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    content = content,
)

/** Icon-only end piece of a split row (Play Store, WhatsApp, Call): the app's own icon, or [fallback]. */
@Composable
private fun IconSegment(shape: Shape, highlight: Boolean, image: ImageBitmap?, fallback: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.width(Sizes.ControlLarge).segment(shape, highlight, onClick), contentAlignment = Alignment.Center) {
        ResultIcon(image, fallback, label)
    }
}

/** An app's own icon in search results, or a plain [fallback] icon when that app isn't installed. */
@Composable
private fun ResultIcon(image: ImageBitmap?, fallback: ImageVector, label: String?) {
    if (image != null) Image(image, label, Modifier.size(32.dp))
    else Icon(fallback, label, tint = MaterialTheme.colorScheme.onSurface)
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
    Row(Modifier.fillMaxWidth().height(Sizes.ControlLarge), horizontalArrangement = Arrangement.spacedBy(SplitGap)) {
        if (playStore) {
            IconSegment(splitShape(end = false), highlight, store, Ic.play, "Search Play Store") { context.playStoreSearch(query) }
        }
        Segment(splitShape(start = !playStore), onClick = { context.webSearch(query) }, Modifier.weight(1f), highlight) {
            ResultIcon(browser, Ic.globe, null)
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
    val source = rememberLaunchSource()
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (highlight) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .combinedClickable(onClick = { source.launch(app) }, onLongClick = { menu = true })
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(app, Sizes.AppIcon, source.modifier, badge = true, themed = LauncherApp.instance.prefs.themedDrawer)
            Spacer(Modifier.height(10.dp))
            Text(
                app.label,
                // No extra letter spacing, so longer names fit before they're cut off.
                style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
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
    Row(Modifier.fillMaxWidth().height(Sizes.ControlLarge), horizontalArrangement = Arrangement.spacedBy(SplitGap)) {
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
            IconSegment(splitShape(top, bottom, start = false, end = !hasCall), highlight, whatsAppIcon, Ic.chat, "WhatsApp", onWhatsApp)
        }
        if (hasCall) IconSegment(splitShape(top, bottom, start = false), highlight, phoneIcon, Ic.phone, "Call", onCall)
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
