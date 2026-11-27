package com.symphonysimper.launcher

import android.Manifest
import android.app.NotificationManager
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.navigationBars
import kotlinx.coroutines.launch
import com.symphonysimper.launcher.ui.theme.pageBackground
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.symphonysimper.launcher.ui.AppIcon
import com.symphonysimper.launcher.ui.Ic
import com.symphonysimper.launcher.ui.SearchField
import com.symphonysimper.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LauncherTheme { SettingsRoot(resumes) } }
    }

    /** Bumped on every resume so rows re-check permissions granted elsewhere. */
    private var resumes by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumes++
    }
}

private enum class Page(val parent: Page?) { Main(null), Dock(Main), AddApp(Dock), Hidden(Main), Engine(Main), Wallpaper(Main) }

/** Shows a message in a snackbar, so feedback uses the app's style rather than a system toast. */
private val LocalNotify = staticCompositionLocalOf<(String) -> Unit> { {} }

@Composable
private fun SettingsRoot(resumes: Int) {
    var page by rememberSaveable { mutableStateOf(Page.Main) }
    val back = { page = page.parent ?: page }
    BackHandler(page != Page.Main, back)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { message ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }
    CompositionLocalProvider(LocalNotify provides notify) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") {
                when (it) {
                    Page.Main -> MainPage(resumes) { page = it }
                    Page.Dock -> DockPage(back) { page = Page.AddApp }
                    Page.AddApp -> AddAppPage(back)
                    Page.Hidden -> HiddenPage(back)
                    Page.Engine -> EnginePage(back)
                    Page.Wallpaper -> WallpaperPage(back)
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            )
        }
    }
}

// ---------------------------------------------------------------- building blocks

@Composable
private fun PageScaffold(
    title: String,
    onBack: (() -> Unit)?,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.pageBackground
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Column(
        Modifier
            .fillMaxSize()
            .background(background)
            .nestedScroll(scroll.nestedScrollConnection)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        LargeFlexibleTopAppBar(
            title = { Text(title) },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) { Icon(Ic.back, "Back") }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = background, scrolledContainerColor = background),
            scrollBehavior = scroll,
        )
        Column(Modifier.weight(1f), content = content)
        bottom?.invoke()
    }
}

/** List row title: one line. */
@Composable
private fun Title(text: String) = Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)

private val itemColors @Composable get() = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceBright)

@Composable
private fun Group(header: String? = null, content: @Composable ColumnScope.() -> Unit) {
    if (header != null) {
        Text(
            header,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp, top = 18.dp, bottom = 8.dp),
        )
    }
    Column(
        Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        content = content,
    )
}

@Composable
private fun Lead(icon: ImageVector) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
}

@Composable
private fun NavRow(i: Int, n: Int, icon: ImageVector, title: String, summary: String? = null, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(i, n),
        colors = itemColors,
        leadingContent = { Lead(icon) },
        supportingContent = summary?.let { { Text(it, maxLines = 1) } },
        trailingContent = { Icon(Ic.chevron, null) },
    ) { Title(title) }
}

@Composable
private fun SwitchRow(
    i: Int,
    n: Int,
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    // Plain onClick, not the checked variant: that one tints the whole row when on.
    SegmentedListItem(
        onClick = { onChange(!checked) },
        shapes = ListItemDefaults.segmentedShapes(i, n),
        colors = itemColors,
        leadingContent = leading,
        trailingContent = { MonoSwitch(checked) },
    ) { Title(title) }
}

@Composable
private fun MonoSwitch(checked: Boolean) = Switch(
    checked = checked,
    onCheckedChange = null,
    thumbContent = if (checked) {
        { Icon(Ic.check, null, Modifier.size(SwitchDefaults.IconSize)) }
    } else null,
)

// ---------------------------------------------------------------- pages

@Composable
private fun MainPage(resumes: Int, open: (Page) -> Unit) {
    val context = LocalContext.current
    val prefs = LauncherApp.instance.prefs
    val repo = LauncherApp.instance.apps
    val notify = LocalNotify.current

    @Suppress("UNUSED_EXPRESSION") resumes // re-read the checks below after returning from system screens
    val isDefault = context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
    val badgeAccess = context.getSystemService(NotificationManager::class.java)
        .isNotificationListenerAccessGranted(ComponentName(context, BadgeService::class.java))
    val contactsAccess = context.hasContactsPermission()

    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        prefs.contacts = it[Manifest.permission.READ_CONTACTS] == true
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(prefs.toJson().toByteArray()) } }
            .onSuccess { notify("Settings saved") }
            .onFailure { notify("Couldn't save the file") }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { prefs.fromJson(context.contentResolver.openInputStream(uri)!!.use { it.reader().readText() }) }
            .onSuccess { notify("Settings restored") }
            .onFailure { notify("That isn't a launcher backup") }
    }

    PageScaffold("Launcher", onBack = null) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            if (!isDefault) {
                Group {
                    NavRow(0, 1, Ic.home, "Set as default launcher") {
                        val role = context.getSystemService(RoleManager::class.java)
                        context.start(role.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Group {
                val dockNames = prefs.dock.mapNotNull { repo.find(it)?.label }.joinToString(", ")
                NavRow(0, 4, Ic.dock, "Dock", dockNames.ifEmpty { "Empty" }) { open(Page.Dock) }
                NavRow(1, 4, Ic.hide, "Hidden apps", "${prefs.hidden.size} hidden") { open(Page.Hidden) }
                SwitchRow(2, 4, "Notification badges", prefs.badges && badgeAccess, { on ->
                    prefs.badges = on
                    if (on && !badgeAccess) {
                        context.start(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                                ComponentName(context, BadgeService::class.java).flattenToString(),
                            ),
                        )
                    }
                }, leading = { Lead(Ic.bell) })
                NavRow(3, 4, Ic.image, "Wallpaper", if (prefs.wallpaperImages) "My images" else "Generated") {
                    open(Page.Wallpaper)
                }
            }
            Group("Search") {
                val engine = if (prefs.engine == "custom") "Custom" else ENGINES[prefs.engine]?.first ?: "Google"
                NavRow(0, 3, Ic.globe, "Search engine", engine) { open(Page.Engine) }
                SwitchRow(1, 3, "Play Store button", prefs.playStore, { prefs.playStore = it }, leading = { Lead(Ic.play) })
                SwitchRow(2, 3, "Contacts", prefs.contacts && contactsAccess, { on ->
                    if (on && !contactsAccess) {
                        askContacts.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE))
                    } else {
                        prefs.contacts = on
                    }
                }, leading = { Lead(Ic.person) })
            }
            Group("More") {
                NavRow(0, 3, Ic.coffee, "Add Stay awake tile") { context.requestAddTile() }
                NavRow(1, 3, Ic.download, "Back up settings") { export.launch("launcher-backup.json") }
                NavRow(2, 3, Ic.upload, "Restore settings") { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            }
        }
    }
}

@Composable
private fun DockPage(onBack: () -> Unit, onAdd: () -> Unit) {
    val prefs = LauncherApp.instance.prefs
    val repo = LauncherApp.instance.apps
    val order = remember(prefs.dock) { prefs.dock.toMutableStateList() }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(1f) }
    val gap = with(LocalDensity.current) { ListItemDefaults.SegmentedGap.toPx() }

    PageScaffold("Dock", onBack) {
        Group {
            order.forEachIndexed { i, key ->
                val app = repo.find(key) ?: return@forEachIndexed
                val currentOrder by rememberUpdatedState(order)
                val isDragged = dragging == key
                SegmentedListItem(
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(i, order.size),
                    colors = if (isDragged) ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else itemColors,
                    modifier = Modifier
                        .onSizeChanged { rowHeight = it.height + gap }
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragged) dragOffset else 0f
                            val s = if (isDragged) 1.03f else 1f
                            scaleX = s
                            scaleY = s
                        }
                        .pointerInput(key) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragging = key; dragOffset = 0f },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val from = currentOrder.indexOf(key)
                                    val to = (from + (dragOffset / rowHeight).roundToInt()).coerceIn(0, currentOrder.lastIndex)
                                    if (to != from) {
                                        currentOrder.add(to, currentOrder.removeAt(from))
                                        dragOffset -= (to - from) * rowHeight
                                    }
                                },
                                onDragEnd = { dragging = null; dragOffset = 0f; prefs.dock = order.toList() },
                                onDragCancel = { dragging = null; dragOffset = 0f; prefs.dock = order.toList() },
                            )
                        },
                    leadingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { prefs.dock = prefs.dock - key }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Ic.remove, "Remove")
                            }
                            AppIcon(app, 40.dp)
                        }
                    },
                    trailingContent = { Icon(Ic.drag, "Hold to reorder") },
                ) { Title(app.label) }
            }
        }
        if (order.size < MAX_DOCK) {
            FilledTonalButton(
                onClick = onAdd,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().padding(12.dp).height(56.dp),
            ) {
                Icon(Ic.add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add app")
            }
        }
    }
}

@Composable
private fun AppListPage(title: String, onBack: () -> Unit, list: LazyListScope.(query: String) -> Unit) {
    var query by remember { mutableStateOf("") }
    PageScaffold(
        title, onBack,
        bottom = {
            SearchField(query, { query = it }, "Search apps", Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp))
        },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) { list(query) }
    }
}

private fun List<App>.matching(query: String) =
    if (query.isBlank()) this else filter { it.label.contains(query.trim(), ignoreCase = true) }

@Composable
private fun AddAppPage(onBack: () -> Unit) {
    val prefs = LauncherApp.instance.prefs
    val all = LauncherApp.instance.apps.all
    AppListPage("Add app", onBack) { query ->
        val apps = all.filter { it.key !in prefs.dock && it.key !in prefs.hidden }.matching(query)
        itemsIndexed(apps, key = { _, app -> app.key }) { i, app ->
            AppRow(app, ListItemDefaults.segmentedShapes(i, apps.size)) {
                prefs.dock = (prefs.dock + app.key).take(MAX_DOCK)
                onBack()
            }
        }
    }
}

@Composable
private fun HiddenPage(onBack: () -> Unit) {
    val prefs = LauncherApp.instance.prefs
    val all = LauncherApp.instance.apps.all
    AppListPage("Hidden apps", onBack) { query ->
        val apps = all.matching(query)
        val hidden = apps.filter { it.key in prefs.hidden }
        val shown = apps.filter { it.key !in prefs.hidden }
        fun section(items: List<App>) = itemsIndexed(items, key = { _, app -> app.key }) { i, app ->
            val isHidden = app.key in prefs.hidden
            SegmentedListItem(
                onClick = { prefs.hidden = if (isHidden) prefs.hidden - app.key else prefs.hidden + app.key },
                shapes = ListItemDefaults.segmentedShapes(i, items.size),
                colors = itemColors,
                leadingContent = { AppIcon(app, 40.dp) },
                trailingContent = { MonoSwitch(isHidden) },
            ) { Title(app.label) }
        }
        section(hidden)
        if (hidden.isNotEmpty() && shown.isNotEmpty()) {
            item(key = "visible-header") {
                Text(
                    "Visible",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp),
                )
            }
        }
        section(shown)
    }
}

@Composable
private fun AppRow(app: App, shapes: ListItemShapes, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = itemColors,
        leadingContent = { AppIcon(app, 40.dp) },
    ) { Title(app.label) }
}

@Composable
private fun EnginePage(onBack: () -> Unit) {
    val prefs = LauncherApp.instance.prefs
    val options = ENGINES.map { (key, value) -> key to value.first } + ("custom" to "Custom")
    PageScaffold("Search engine", onBack) {
        Group {
            options.forEachIndexed { i, (key, name) ->
                SegmentedListItem(
                    onClick = { prefs.engine = key },
                    shapes = ListItemDefaults.segmentedShapes(i, options.size),
                    colors = itemColors,
                    leadingContent = { RadioButton(selected = prefs.engine == key, onClick = null) },
                    supportingContent = if (key == "custom") {
                        {
                            OutlinedTextField(
                                value = prefs.customUrl,
                                onValueChange = {
                                    prefs.customUrl = it.trim()
                                    if (it.isNotBlank()) prefs.engine = "custom"
                                },
                                placeholder = { Text("https://…?q=%s") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            )
                        }
                    } else null,
                ) { Title(name) }
            }
        }
    }
}

@Composable
private fun WallpaperPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = LauncherApp.instance.prefs
    val notify = LocalNotify.current
    var pickingDark by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                PebblesWallpaper.wallpaperFile(context, pickingDark).outputStream().use { input.copyTo(it) }
            }
        }.onSuccess {
            prefs.wallpaperVersion++
        }.onFailure { notify("Couldn't use that image") }
    }
    fun pick(dark: Boolean) {
        pickingDark = dark
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    PageScaffold("Wallpaper", onBack) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            ModeButton("Generated", checked = !prefs.wallpaperImages, ButtonGroupDefaults.connectedLeadingButtonShapes()) {
                prefs.wallpaperImages = false
            }
            ModeButton("My images", checked = prefs.wallpaperImages, ButtonGroupDefaults.connectedTrailingButtonShapes()) {
                prefs.wallpaperImages = true
            }
        }
        Row(
            Modifier.fillMaxWidth().height(320.dp).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!prefs.wallpaperImages) {
                GeneratedPreview(dark = false)
                GeneratedPreview(dark = true)
            } else if (prefs.sameImage) {
                ImageSlot("Light & dark", dark = false, version = prefs.wallpaperVersion) { pick(false) }
            } else {
                ImageSlot("Light", dark = false, version = prefs.wallpaperVersion) { pick(false) }
                ImageSlot("Dark", dark = true, version = prefs.wallpaperVersion) { pick(true) }
            }
        }
        if (prefs.wallpaperImages) {
            Group { SwitchRow(0, 1, "Same image for both", prefs.sameImage, { prefs.sameImage = it }) }
        }
        Spacer(Modifier.weight(1f))
        val component = ComponentName(context, PebblesWallpaper::class.java)
        val active = WallpaperManager.getInstance(context).wallpaperInfo?.component == component
        if (!active) {
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    context.start(
                        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component),
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            ) { Text("Set wallpaper") }
        }
    }
}

@Composable
private fun RowScope.ModeButton(text: String, checked: Boolean, shapes: ToggleButtonShapes, onClick: () -> Unit) {
    ToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        shapes = shapes,
        colors = ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.weight(1f).height(52.dp).semantics { role = Role.RadioButton },
    ) {
        if (checked) {
            Icon(Ic.check, null, Modifier.size(ToggleButtonDefaults.IconSize))
            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
        }
        Text(text)
    }
}

@Composable
private fun RowScope.GeneratedPreview(dark: Boolean) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { Pebbles.draw(it.nativeCanvas, size.width, size.height, dark) }
        }
        SlotLabel(if (dark) "Dark" else "Light")
    }
}

@Composable
private fun SlotLabel(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .padding(12.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun RowScope.ImageSlot(label: String, dark: Boolean, version: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, version, dark) {
        value = withContext(Dispatchers.IO) {
            PebblesWallpaper.decodeSampled(PebblesWallpaper.wallpaperFile(context, dark), 300, 600)?.asImageBitmap()
        }
    }
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomCenter,
    ) {
        image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (image == null) {
            Icon(Ic.add, null, Modifier.align(Alignment.Center).size(32.dp))
        }
        SlotLabel(label)
    }
}

