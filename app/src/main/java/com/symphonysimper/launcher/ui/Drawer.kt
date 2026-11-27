package com.symphonysimper.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.symphonysimper.launcher.App
import com.symphonysimper.launcher.Contact
import com.symphonysimper.launcher.LauncherApp
import com.symphonysimper.launcher.appInfo
import com.symphonysimper.launcher.call
import com.symphonysimper.launcher.launch
import com.symphonysimper.launcher.openContact
import com.symphonysimper.launcher.playStoreSearch
import com.symphonysimper.launcher.searchContacts
import com.symphonysimper.launcher.webSearch
import com.symphonysimper.launcher.whatsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val COLUMNS = 5

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

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (query.isBlank()) AppGrid(apps, onClose)
                else Results(appResults, contacts)
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val searching = query.isNotBlank()
                if (searching && prefs.playStore) {
                    val store = remember(LauncherApp.instance.apps.all) {
                        LauncherApp.instance.apps.all.firstOrNull { it.pkg == "com.android.vending" }
                    }
                    FilledTonalIconButton(onClick = { context.playStoreSearch(query) }, modifier = Modifier.size(56.dp)) {
                        if (store != null) Image(store.icon, "Search Play Store", Modifier.size(30.dp))
                        else Icon(Ic.play, "Search Play Store")
                    }
                }
                SearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = "Search apps, contacts, web",
                    focusRequester = focus,
                    onSearch = ::onEnter,
                    modifier = Modifier.weight(1f),
                )
                if (searching) {
                    FilledIconButton(onClick = { context.webSearch(query) }, modifier = Modifier.size(56.dp)) {
                        Icon(Ic.globe, "Search the web")
                    }
                }
            }
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
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(apps, key = { it.key }) { AppCell(it) }
    }
}

/** Results sit at the bottom, nearest the search field: contacts, then apps right above the field. */
@Composable
private fun Results(apps: List<App>, contacts: List<Contact>) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState(), reverseScrolling = true)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (contacts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    contacts.forEachIndexed { i, contact ->
                        ContactRow(
                            contact = contact,
                            highlight = apps.isEmpty() && i == 0,
                            index = i,
                            count = contacts.size,
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
            AppIcon(app, 52.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                app.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        ExpressiveMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Hide") },
                leadingIcon = { Icon(Ic.hide, null) },
                onClick = {
                    menu = false
                    val prefs = LauncherApp.instance.prefs
                    prefs.hidden = prefs.hidden + app.key
                },
            )
            DropdownMenuItem(
                text = { Text("App info") },
                leadingIcon = { Icon(Ic.info, null) },
                onClick = { menu = false; context.appInfo(app) },
            )
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    highlight: Boolean,
    index: Int,
    count: Int,
    onOpen: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SegmentedListItem(
        onClick = onOpen,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = if (highlight) colors.secondaryContainer else colors.surfaceContainer,
        ),
        leadingContent = {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(40)).background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(contact.name.take(1).uppercase(), color = colors.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
            }
        },
        supportingContent = contact.phoneLabel?.let { { Text(it) } },
        trailingContent = {
            // Call stays in the same spot whether or not the WhatsApp button is there.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (contact.whatsAppDataId != null) {
                    FilledTonalIconButton(onClick = onWhatsApp) { Icon(Ic.chat, "WhatsApp") }
                } else {
                    Spacer(Modifier.size(40.dp))
                }
                if (contact.phone != null) {
                    FilledIconButton(onClick = onCall) { Icon(Ic.phone, "Call") }
                }
            }
        },
    ) {
        Text(contact.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
