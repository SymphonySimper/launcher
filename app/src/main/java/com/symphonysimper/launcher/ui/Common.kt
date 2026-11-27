package com.symphonysimper.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.graphics.Rect
import android.view.View
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalView
import com.symphonysimper.launcher.App
import com.symphonysimper.launcher.launch
import com.symphonysimper.launcher.open
import com.symphonysimper.launcher.LauncherApp
import com.symphonysimper.launcher.ui.theme.Sizes

class LaunchSource(private val view: View) {
    private var icon: LayoutCoordinates? = null

    val modifier = Modifier.onPlaced { icon = it }

    private val bounds get() = icon?.takeIf { it.isAttached }?.boundsInRoot()?.run { Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt()) }

    fun launch(app: App) = view.context.launch(app, view, bounds)

    fun open(intent: Intent, fallback: Intent? = null) = view.context.open(intent, fallback, view, bounds)
}

@Composable
fun rememberLaunchSource(): LaunchSource {
    val view = LocalView.current
    return remember(view) { LaunchSource(view) }
}

@Composable
fun AppIcon(
    app: App,
    size: Dp,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    badge: Boolean = false,
    themed: Boolean = false,
) {
    val prefs = LauncherApp.instance.prefs
    val dot = badge && prefs.badges && (Badges.counts[app.pkg] ?: 0) > 0
    val color = MaterialTheme.colorScheme.primary
    val mono = if (themed) LauncherApp.instance.apps.monochrome(app.key) else null
    Box(
        modifier.drawWithContent {
            drawContent()
            if (dot) drawCircle(color, DotRadius * this.size.width, dotCenter())
        },
    ) {
        val notched = iconModifier
            .size(size)
            // Its own layer, so the notch clears the icon only and shows what's behind.
            .graphicsLayer { compositingStrategy = if (dot) CompositingStrategy.Offscreen else CompositingStrategy.Auto }
            .drawWithContent {
                drawContent()
                if (dot) drawCircle(Color.Black, (DotRadius + DotGap) * this.size.width, dotCenter(), blendMode = BlendMode.Clear)
            }
        if (mono != null) {
            val dark = isSystemInDarkTheme()
            val background = colorResource(if (dark) android.R.color.system_neutral1_800 else android.R.color.system_accent1_100)
            val foreground = colorResource(if (dark) android.R.color.system_accent1_100 else android.R.color.system_neutral2_700)
            Image(
                mono,
                contentDescription = null,
                colorFilter = ColorFilter.tint(foreground),
                modifier = notched.background(background, SquircleShape),
            )
        } else {
            Image(app.icon, contentDescription = null, modifier = notched)
        }
    }
}

private const val DotRadius = 0.125f
private const val DotGap = 0.045f

private fun DrawScope.dotCenter() = Offset(size.width * (1 - DotRadius), size.height * DotRadius)

@Composable
fun keyboardOpen(): Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0

@Composable
fun searchBarBottom(): Dp = if (keyboardOpen()) 10.dp else 24.dp

@Composable
fun searchBarSpace(): Dp = 10.dp + Sizes.Control + searchBarBottom()

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
    onSearch: () -> Unit = {},
    color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier.focusRequester(focusRequester),
        decorationBox = { field ->
            Row(
                Modifier
                    .height(Sizes.Control)
                    .clip(CircleShape)
                    .background(color)
                    .padding(start = 18.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value.isEmpty()) {
                    Icon(Ic.search, null, tint = iconColor)
                    Spacer(Modifier.width(14.dp))
                }
                Box(Modifier.weight(1f)) { field() }
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(Sizes.MinTouch)) {
                        Icon(Ic.close, "Clear", tint = contentColor)
                    }
                } else {
                    Spacer(Modifier.width(14.dp))
                }
            }
        },
    )
}

class MenuEntry(val icon: ImageVector, val text: String, val onClick: () -> Unit)

private val MenuMinWidth = 200.dp

private val PillGap = 8.dp
private val ArrowSize = DpSize(22.dp, 10.dp)

private class MenuPosition(val inset: Int, val gap: Int, val margin: Int, val top: Int) : PopupPositionProvider {
    var below by mutableStateOf(false)
    var arrowX by mutableIntStateOf(0)

    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val cx = anchorBounds.center.x
        val w = popupContentSize.width
        val left = if (cx - inset + w <= windowSize.width - margin) cx - inset else cx + inset - w
        val x = left.coerceIn(margin, (windowSize.width - margin - w).coerceAtLeast(margin))
        val above = anchorBounds.top - gap - popupContentSize.height
        below = above < top
        arrowX = cx - x
        return IntOffset(x, if (below) anchorBounds.bottom + gap else above)
    }
}

fun splitShape(top: Boolean = true, bottom: Boolean = true, start: Boolean = true, end: Boolean = true): Shape {
    val big = 24.dp
    val small = 4.dp
    return RoundedCornerShape(
        topStart = if (top && start) big else small,
        topEnd = if (top && end) big else small,
        bottomStart = if (bottom && start) big else small,
        bottomEnd = if (bottom && end) big else small,
    )
}

val SplitGap = 2.dp

@Composable
fun LauncherMenu(
    entries: List<MenuEntry>,
    arrow: Boolean,
    onDismiss: () -> Unit,
    pill: MenuEntry? = null,
    pillOn: Boolean = false,
) {
    val density = LocalDensity.current
    val statusBar = WindowInsets.statusBars.getTop(density)
    val position = remember(arrow, statusBar) {
        with(density) {
            MenuPosition(
                inset = 40.dp.roundToPx(),
                gap = (if (arrow) 2.dp else 32.dp).roundToPx(),
                margin = 8.dp.roundToPx(),
                top = statusBar + 8.dp.roundToPx(),
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(160)) }
    val color = MaterialTheme.colorScheme.surfaceBright
    Popup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(
            Modifier
                .widthIn(min = MenuMinWidth)
                .width(IntrinsicSize.Max)
                .padding(vertical = 4.dp)
                .graphicsLayer {
                    alpha = progress.value
                    scaleX = 0.92f + 0.08f * progress.value
                    scaleY = scaleX
                    transformOrigin = TransformOrigin(position.arrowX / size.width, if (position.below) 0f else 1f)
                },
            verticalArrangement = Arrangement.spacedBy(SplitGap),
        ) {
            if (pill != null && position.below) {
                MenuPill(pill, pillOn, color, true.takeIf { arrow }, position.arrowX, onDismiss)
                Spacer(Modifier.height(PillGap - SplitGap * 2))
            }
            entries.forEachIndexed { i, entry ->
                val shape = splitShape(top = i == 0, bottom = i == entries.lastIndex)
                val arrowUp = when {
                    !arrow || pill != null -> null
                    position.below -> true.takeIf { i == 0 }
                    else -> false.takeIf { i == entries.lastIndex }
                }
                MenuCard(shape, color, arrowUp, position.arrowX) { MenuRow(entry, onDismiss) }
            }
            if (pill != null && !position.below) {
                Spacer(Modifier.height(PillGap - SplitGap * 2))
                MenuPill(pill, pillOn, color, false.takeIf { arrow }, position.arrowX, onDismiss)
            }
        }
    }
}

@Composable
private fun MenuPill(entry: MenuEntry, on: Boolean, color: Color, arrowUp: Boolean?, arrowX: Int, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    MenuCard(CircleShape, if (on) colors.primary else color, arrowUp, arrowX) {
        MenuRow(entry, onDismiss, if (on) colors.onPrimary else colors.onSurface, toggle = on)
    }
}

@Composable
private fun MenuCard(shape: Shape, color: Color, arrowUp: Boolean?, arrowX: Int, content: @Composable ColumnScope.() -> Unit) {
    val outline = if (arrowUp == null) shape else remember(shape, arrowUp, arrowX) { ArrowShape(shape, arrowX.toFloat(), arrowUp) }
    val room = if (arrowUp == true) PaddingValues(top = ArrowSize.height) else if (arrowUp == false) PaddingValues(bottom = ArrowSize.height) else PaddingValues()
    Column(Modifier.fillMaxWidth().shadow(3.dp, outline).background(color, outline).clip(outline).padding(room), content = content)
}

private class ArrowShape(private val base: Shape, private val arrowX: Float, private val up: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = with(density) { ArrowSize.width.toPx() } / 2
        val h = with(density) { ArrowSize.height.toPx() }
        val fillet = with(density) { 4.dp.toPx() }
        val body = Path().apply {
            addOutline(base.createOutline(Size(size.width, size.height - h), layoutDirection, density))
            if (up) translate(Offset(0f, h))
        }
        val out = if (up) -1f else 1f
        val edge = if (up) h else size.height - h
        val tip = Offset(arrowX, edge + out * h)
        val left = Offset(arrowX - w, edge)
        val right = Offset(arrowX + w, edge)
        fun Offset.towards(to: Offset, t: Float) = this + (to - this) * t
        val arrow = Path().apply {
            moveTo(left.x - fillet, edge)
            left.towards(tip, 0.35f).let { quadraticTo(left.x, left.y, it.x, it.y) }
            tip.towards(left, 0.35f).let { lineTo(it.x, it.y) }
            tip.towards(right, 0.35f).let { quadraticTo(tip.x, tip.y, it.x, it.y) }
            right.towards(tip, 0.35f).let { lineTo(it.x, it.y) }
            quadraticTo(right.x, right.y, right.x + fillet, edge)
            lineTo(right.x + fillet, edge - out * fillet)
            lineTo(left.x - fillet, edge - out * fillet)
            close()
        }
        return Outline.Generic(Path.combine(PathOperation.Union, body, arrow))
    }
}

@Composable
private fun MenuRow(
    entry: MenuEntry,
    onDismiss: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface,
    toggle: Boolean? = null,
) {
    val onClick = { onDismiss(); entry.onClick() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(Sizes.Control)
            .then(
                if (toggle != null) Modifier.toggleable(toggle, role = Role.Switch) { onClick() }
                else Modifier.clickable(onClick = onClick),
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(entry.icon, null, Modifier.size(Sizes.Icon), tint = color)
        Text(entry.text, style = MaterialTheme.typography.bodyLarge, color = color, maxLines = 1, softWrap = false)
    }
}
