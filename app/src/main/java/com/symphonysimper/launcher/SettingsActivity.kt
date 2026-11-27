package com.symphonysimper.launcher

import android.Manifest
import android.app.NotificationManager
import android.app.Activity
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.unit.Dp
import com.symphonysimper.launcher.ui.theme.Sizes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Shape
import com.symphonysimper.launcher.ui.SplitGap
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.FilledIconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import com.symphonysimper.launcher.ui.theme.isDark
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalWindowInfo
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
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(Sizes.MinTouch)) {
                        Icon(Ic.back, "Back")
                    }
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
private fun NavRow(i: Int, n: Int, icon: ImageVector, title: String, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(i, n),
        colors = itemColors,
        leadingContent = { Lead(icon) },
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
    val notify = LocalNotify.current

    @Suppress("UNUSED_EXPRESSION") resumes // re-read the checks below after returning from system screens
    val isDefault = context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
    val badgeAccess = context.getSystemService(NotificationManager::class.java)
        .isNotificationListenerAccessGranted(ComponentName(context, BadgeService::class.java))
    val contactsAccess = context.hasContactsPermission()

    // The role dialog needs to know who is asking, which it only learns when started for a result.
    // If it's dismissed or Android won't show it (after two refusals), open the system's home app picker.
    val askHome = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val role = context.getSystemService(RoleManager::class.java)
        if (!role.isRoleHeld(RoleManager.ROLE_HOME)) context.start(Intent(Settings.ACTION_HOME_SETTINGS))
    }
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
                        askHome.launch(context.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Group {
                NavRow(0, 4, Ic.dock, "Dock") { open(Page.Dock) }
                NavRow(1, 4, Ic.hide, "Hidden apps") { open(Page.Hidden) }
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
                NavRow(3, 4, Ic.image, "Wallpaper") {
                    open(Page.Wallpaper)
                }
            }
            Group("Search") {
                NavRow(0, 3, Ic.globe, "Search engine") { open(Page.Engine) }
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
                            IconButton(
                                onClick = { prefs.dock = prefs.dock - key },
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.size(Sizes.MinTouch),
                            ) {
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
                modifier = Modifier.fillMaxWidth().padding(12.dp).height(Sizes.Control),
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
            SearchField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp))
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

/**
 * The wallpaper fills the page so it can be judged as it will look. Controls float over it in the
 * system theme: the source at the top, light / dark and image actions in a toolbar at the bottom.
 */
@Composable
private fun WallpaperPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = LauncherApp.instance.prefs
    val notify = LocalNotify.current
    var showDark by rememberSaveable { mutableStateOf(PebblesWallpaper.isDark(context)) }
    val images = prefs.wallpaperImages
    val linked = images && prefs.sameImage
    // With one image for both modes it lives in the light slot.
    val slotDark = showDark && !linked

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                PebblesWallpaper.wallpaperFile(context, slotDark).outputStream().use { input.copyTo(it) }
            }
        }.onSuccess {
            prefs.wallpaperVersion++
        }.onFailure { notify("Couldn't use that image") }
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    val window = LocalWindowInfo.current.containerSize
    val lightImage = rememberWallpaperImage(dark = false, prefs.wallpaperVersion, window)
    val darkImage = rememberWallpaperImage(dark = true, prefs.wallpaperVersion, window)
    val shown = if (images) Preview.Photo(slotDark, if (slotDark) darkImage else lightImage) else Preview.Pebbles(showDark)
    val previewDark = when (shown) {
        is Preview.Pebbles -> shown.dark
        // An empty slot shows the page's own surface.
        is Preview.Photo -> if (shown.image != null) shown.image.isMostlyDark() else MaterialTheme.colorScheme.isDark
    }
    SystemBarIcons(lightIcons = previewDark)

    Box(Modifier.fillMaxSize()) {
        WallpaperArt(shown, Modifier.fillMaxSize().then(if (images) Modifier.clickable(onClick = pick) else Modifier))

        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(Ic.back, "Back", size = Sizes.Control, onClick = onBack)
            PillGroup(Modifier.weight(1f).height(Sizes.Control)) {
                SourceButton("Pebbles", Ic.shapes, checked = !images) { prefs.wallpaperImages = false }
                SourceButton("My images", Ic.image, checked = images) { prefs.wallpaperImages = true }
            }
        }

        val component = ComponentName(context, PebblesWallpaper::class.java)
        val active = WallpaperManager.getInstance(context).wallpaperInfo?.component == component
        // Bottom controls sit in thumb reach, lifted clear of the gesture bar.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 12.dp, end = 12.dp, bottom = Sizes.ThumbLift),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A split button: the light / dark group leads, the pencil trails, joined by a narrow gap.
            Row(horizontalArrangement = Arrangement.spacedBy(SplitGap)) {
                PillGroup(Modifier.height(Sizes.ControlLarge), shape = splitPill(end = !images)) {
                    PillToggle(Ic.sun, "Light", checked = !showDark || linked) { showDark = false }
                    if (images) {
                        PillToggle(
                            Ic.link, "Same image for both", checked = linked,
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        ) { prefs.sameImage = !prefs.sameImage }
                    }
                    PillToggle(Ic.moon, "Dark", checked = showDark || linked) { showDark = true }
                }
                if (images) {
                    val what = if (linked) "image" else if (slotDark) "dark image" else "light image"
                    val verb = if (shown is Preview.Photo && shown.image == null) "Choose" else "Change"
                    RoundButton(Ic.edit, "$verb $what", size = Sizes.ControlLarge, shape = splitPill(start = false), onClick = pick)
                }
            }
            if (!active) {
                RoundButton(
                    Ic.check, "Set wallpaper",
                    size = Sizes.ControlLarge,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    context.start(
                        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component),
                    )
                }
            }
        }
    }
}

/** A pill whose sides can meet a neighbour, as in a split button: fully round outside, 4dp where they join. */
private fun splitPill(start: Boolean = true, end: Boolean = true): Shape {
    val round = CornerSize(50)
    val joint = CornerSize(4.dp)
    return RoundedCornerShape(
        topStart = if (start) round else joint,
        topEnd = if (end) round else joint,
        bottomEnd = if (end) round else joint,
        bottomStart = if (start) round else joint,
    )
}

/** An icon button on the system surface, for controls floating over the wallpaper. */
@Composable
private fun RoundButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    shape: Shape = CircleShape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        shape = shape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = containerColor, contentColor = contentColor),
        modifier = Modifier.size(size),
    ) { Icon(icon, label, Modifier.size(Sizes.Icon)) }
}

/** A connected button group: pill-shaped toggles inside one pill on the system surface. */
@Composable
private fun PillGroup(modifier: Modifier = Modifier, shape: Shape = CircleShape, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/** Sets the status and navigation bar icons to read against the preview, restoring them on leaving. */
@Composable
private fun SystemBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    DisposableEffect(lightIcons) {
        val window = (view.context as Activity).window
        val bars = WindowCompat.getInsetsController(window, view)
        val status = bars.isAppearanceLightStatusBars
        val navigation = bars.isAppearanceLightNavigationBars
        bars.isAppearanceLightStatusBars = !lightIcons
        bars.isAppearanceLightNavigationBars = !lightIcons
        onDispose {
            bars.isAppearanceLightStatusBars = status
            bars.isAppearanceLightNavigationBars = navigation
        }
    }
}

@Composable
private fun RowScope.SourceButton(text: String, icon: ImageVector, checked: Boolean, onClick: () -> Unit) {
    ToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        shapes = ToggleButtonShapes(CircleShape, ToggleButtonDefaults.pressedShape, CircleShape),
        colors = ToggleButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.weight(1f).fillMaxHeight().semantics { role = Role.RadioButton },
    ) {
        Icon(icon, null, Modifier.size(ToggleButtonDefaults.IconSize))
        Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
        Text(text, maxLines = 1)
    }
}

@Composable
private fun PillToggle(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    checkedColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    checkedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: () -> Unit,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        shapes = ToggleButtonShapes(CircleShape, ToggleButtonDefaults.pressedShape, CircleShape),
        colors = ToggleButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = checkedColor,
            checkedContentColor = checkedContentColor,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.width(Sizes.Control).fillMaxHeight(),
    ) { Icon(icon, label, Modifier.size(Sizes.Icon)) }
}

/** What the wallpaper preview shows; a change crossfades. */
private sealed interface Preview {
    data class Pebbles(val dark: Boolean) : Preview
    data class Photo(val dark: Boolean, val image: ImageBitmap?) : Preview
}

/** Loads the user's light or dark wallpaper image to cover [size], reloading when [version] changes. */
@Composable
private fun rememberWallpaperImage(dark: Boolean, version: Int, size: IntSize): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, version, dark, size) {
        value = withContext(Dispatchers.IO) {
            val file = PebblesWallpaper.wallpaperFile(context, dark)
            PebblesWallpaper.decodeSampled(file, size.width.coerceAtLeast(1), size.height.coerceAtLeast(1))?.asImageBitmap()
        }
    }
    return image
}

/** Whether the top of the image, where the status bar icons sit, is dark. Samples a coarse grid. */
private fun ImageBitmap.isMostlyDark(): Boolean {
    val bitmap = asAndroidBitmap()
    var sum = 0f
    var n = 0
    for (x in 0 until 8) for (y in 0 until 3) {
        val c = bitmap.getPixel(bitmap.width * (2 * x + 1) / 16, bitmap.height * (2 * y + 1) / 60)
        sum += Color(c).luminance()
        n++
    }
    return sum / n < 0.5f
}

@Composable
private fun WallpaperArt(shown: Preview, modifier: Modifier) {
    Crossfade(shown, modifier, label = "wallpaper") { state ->
        when (state) {
            is Preview.Pebbles -> Canvas(Modifier.fillMaxSize()) {
                drawIntoCanvas { Pebbles.draw(it.nativeCanvas, size.width, size.height, state.dark) }
            }
            is Preview.Photo -> if (state.image != null) {
                Image(state.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Ic.add, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.dark) "Choose a dark image" else "Choose an image",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
