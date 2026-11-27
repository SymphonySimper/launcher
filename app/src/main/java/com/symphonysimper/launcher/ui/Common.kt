package com.symphonysimper.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Badge
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
import com.symphonysimper.launcher.App
import com.symphonysimper.launcher.LauncherApp

@Composable
fun AppIcon(app: App, size: Dp, modifier: Modifier = Modifier) {
    val prefs = LauncherApp.instance.prefs
    val count = if (prefs.badges) Badges.counts[app.pkg] ?: 0 else 0
    Box(modifier) {
        Image(app.icon, contentDescription = null, modifier = Modifier.size(size))
        if (count > 0) {
            Badge(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)) {
                Text(if (count > 99) "99+" else count.toString())
            }
        }
    }
}

@Composable
fun keyboardOpen(): Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0

/** Gap under the launcher's search bar: room above the gesture bar, less when the keyboard is up. */
@Composable
fun searchBarBottom(): Dp = if (keyboardOpen()) 10.dp else 24.dp

/** Height the launcher's bottom search bar takes up, so home and drawer content stay clear of it. */
@Composable
fun searchBarSpace(): Dp = 10.dp + 56.dp + searchBarBottom()

/** Pill search field used in the launcher and in app lists. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
    onSearch: () -> Unit = {},
    color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier.focusRequester(focusRequester),
        decorationBox = { field ->
            Row(
                Modifier
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(color)
                    .padding(start = 18.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value.isEmpty()) {
                    Icon(Ic.search, null, tint = colors.onSurfaceVariant)
                    Spacer(Modifier.width(14.dp))
                }
                Box(Modifier.weight(1f)) { field() }
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }, shapes = IconButtonDefaults.shapes()) { Icon(Ic.close, "Clear") }
                } else {
                    Spacer(Modifier.width(14.dp))
                }
            }
        },
    )
}

/** Material 3 Expressive menu: one rounded group in a popup, as in the mockup. */
class MenuEntry(val icon: ImageVector, val text: String, val onClick: () -> Unit)

private val MenuMinWidth = 200.dp
private val ArrowSize = DpSize(18.dp, 9.dp)

/** Places the menu above its anchor (below when there's no room), lined up like Launcher3's popups. */
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

/**
 * Split shape used across the launcher (menus, search results): big outer corners on the edges
 * of a group, small inner corners where pieces meet.
 */
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

/** Lawnchair-style long-press menu, anchored to the composable it's placed in. [arrow] points at the anchor. */
@Composable
fun LauncherMenu(entries: List<MenuEntry>, arrow: Boolean, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val statusBar = WindowInsets.statusBars.getTop(density)
    val position = remember(arrow, statusBar) {
        with(density) {
            MenuPosition(
                inset = 26.dp.roundToPx(),
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
            if (arrow && position.below) MenuArrow(position.arrowX, up = true, color)
            entries.forEachIndexed { i, entry ->
                MenuCard(splitShape(top = i == 0, bottom = i == entries.lastIndex), color) { MenuRow(entry, onDismiss) }
            }
            if (arrow && !position.below) MenuArrow(position.arrowX, up = false, color)
        }
    }
}

@Composable
private fun MenuCard(shape: Shape, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().shadow(3.dp, shape).background(color, shape).clip(shape), content = content)
}

@Composable
private fun MenuRow(entry: MenuEntry, onDismiss: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onDismiss(); entry.onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(entry.icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
        Text(entry.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun MenuArrow(centerX: Int, up: Boolean, color: Color) {
    val shape = remember(up) {
        GenericShape { size, _ ->
            if (up) {
                moveTo(size.width / 2, 0f); lineTo(size.width, size.height); lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height)
            }
            close()
        }
    }
    Box(
        Modifier
            .offset { IntOffset(centerX - ArrowSize.width.roundToPx() / 2, SplitGap.roundToPx() * if (up) 1 else -1) }
            .size(ArrowSize)
            .background(color, shape),
    )
}
