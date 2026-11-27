package com.symphonysimper.launcher.ui

import androidx.compose.foundation.Image
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
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.MenuDefaults
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

/** Pill search field used in the drawer and in app lists. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
    onSearch: () -> Unit = {},
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
                    .background(colors.surfaceContainerHighest)
                    .padding(start = 18.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value.isEmpty()) {
                    Icon(Ic.search, null, tint = colors.onSurfaceVariant)
                    Spacer(Modifier.width(14.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, color = colors.onSurfaceVariant, maxLines = 1)
                    field()
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) { Icon(Ic.close, "Clear") }
                } else {
                    Spacer(Modifier.width(14.dp))
                }
            }
        },
    )
}

/** Material 3 Expressive menu: one rounded group in a popup. */
@Composable
fun ExpressiveMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1), content = content)
    }
}
