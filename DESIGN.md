# DESIGN.md

How the launcher looks and behaves, so new UI matches what's already built. Read this before
touching anything visible. When it doesn't cover a case, copy the nearest existing screen rather
than inventing something, and add the new rule here.

The look is **Material 3 Expressive with Material You colours**: soft, rounded, flat surfaces on a
tinted page, with controls sized and placed for one-handed use.

## Principles

1. **Nothing has a sharp corner.** Every surface, button, highlight, menu arrow and drag shadow is
   rounded. If a shape has a point, round it.
2. **Thumb first.** Things people tap often sit near the bottom. Search results grow upwards from
   the search bar, floating bottom controls are lifted above the gesture bar, and the result that
   Enter would open is the one nearest the thumb.
3. **Follow the system.** Colours come from the phone's wallpaper palette and dark mode. Never
   hard-code a UI colour.
4. **Calm surfaces.** Flat by default. Shadows only where something floats over content that could
   be the same colour as it.
5. **Fast.** Opening the drawer, dragging and launching must stay smooth. Animated values are read
   while drawing, not during composition.

## Colour

Use `MaterialTheme.colorScheme` roles only (`ui/theme/Theme.kt` sets up dynamic colour). The roles
have fixed jobs:

| Role | Used for |
|---|---|
| `pageBackground` (extension in `Theme.kt`) | The page behind everything: drawer, settings pages. Tinted in light, near-black in dark. |
| `surfaceBright` | Anything sitting on the page: list rows, the settings search bar, menu cards, result segments, floating controls. |
| `secondaryContainer` / `onSecondaryContainer` | Selected or highlighted: the result Enter will open, a checked toggle, the row being dragged, the circle behind a settings row icon. Also the launcher's search bar (see below). |
| `primary` / `onPrimary` | The single main action on a screen ("Set wallpaper") or a strong on/off state ("Same image for both"). Use sparingly. |
| `primaryContainer` / `onPrimaryContainer` | Contact initial avatars. |
| `onSurface` | Main text and icons. |
| `onSurfaceVariant` | Secondary text and icons: section headers, unselected toggles, the search icon. |

**Exception: the launcher's search bar** (home and drawer, `SearchField` in `MainActivity.kt`) is
tonal: `secondaryContainer` with its icon, text and clear button in `onSecondaryContainer`. It sits
straight on the wallpaper, and `surfaceBright` is almost the same white as Pebbles, so it vanished.
Tonal follows Material You, so it also suits a photo wallpaper. Search bars on settings pages stay
on the default colours.

`ColorScheme.isDark` tells you which mode you're in. The only hard-coded colours in the app are the
Pebbles wallpaper palette (see [Wallpaper](#wallpaper)).

## Type

- Everything is **regular weight**. `RegularType` in `Theme.kt` removes Material's medium weight,
  because some phones draw it nearly bold. Don't set `FontWeight.Medium` or `Bold`.
- Row and result text: `bodyLarge`. App labels under icons: `bodyMedium` with no letter spacing,
  so names like "HDFC Bank App" fit. Section headers: `labelLarge` in `onSurfaceVariant`.
- Row titles are one line with an ellipsis (`Title()` in settings).
- The home clock is the one custom style (84sp, tight letter spacing). Don't add more.

## Sizes

Always use the tokens in `ui/theme/Sizes.kt`, never a literal dp for something tappable. If a size
is missing, add a token there.

| Token | Size | For |
|---|---|---|
| `MinTouch` | 48dp | Smallest anything tappable can be; inline icon buttons (clear, back, remove). |
| `Control` | 56dp | Buttons, toggles, menu rows, the search field. |
| `ControlLarge` | 64dp | Search result rows and controls floating over content. |
| `Icon` | 24dp | Icons. |
| `Chip` | 32dp | A chip's visible height; its touch area still grows to 48dp. |
| `AppIcon` | 60dp | App icons in the dock and the drawer, the same size in both so nothing jumps when the drawer opens (40dp inside settings rows). Icons are rendered at this size. |
| `ThumbLift` | 40dp | Extra space under bottom floating controls, above the gesture bar. |

The drawer is roomy, matched to Lawnchair's: 60dp icons, a 10dp gap to the label, 8dp padding inside
each cell and 12dp between rows (about 120dp a row), with 24dp above the first row.

Spacing that keeps coming up: 12dp side padding for lists and groups, 16dp padding inside rows,
`SplitGap` (2dp) between joined pieces, `ListItemDefaults.SegmentedGap` between settings rows.

## Shapes

| Shape | Where | Defined in |
|---|---|---|
| Pill (`CircleShape`) | Buttons, the search field, button groups, toggles, round icon buttons. | Compose |
| `splitShape(top, bottom, start, end)` | Pieces of a group of rows or a split row: 24dp outer corners, 4dp where pieces meet. Menus and search results. | `ui/Common.kt` |
| `splitPill(start, end)` | The same idea for pills: fully round outside, 4dp at the join. Wallpaper page split button. | `SettingsActivity.kt` |
| `ListItemDefaults.segmentedShapes(i, n)` | Settings list rows: round at the ends of a group, tighter in the middle. | Material 3 |
| `SquircleShape` | Clipping touch ripples on app icons, matching the icon shape. | `ui/Icons.kt` |
| 20dp rounded rect | Highlight behind an app cell in the drawer. | `ui/Drawer.kt` |
| 28dp top corners → 0 | The drawer sheet while it moves; square once fully open. Clipped in `graphicsLayer`. | `MainActivity.kt` |
| 16dp rounded rect | Text fields, the shadow of a dragged row. | |

Buttons use Material's expressive press shapes so they squish when pressed: pass
`shapes = IconButtonDefaults.shapes()` / `ButtonDefaults.shapes()`, and `ToggleButton` with
`PillShapes`.

### Vocabulary

The user describes UI with these names:

- **Split button** ("split thingy"): pieces joined by a 2dp gap, round on the outside and 4dp where
  they meet. The search result rows (contact | WhatsApp | Call, Play Store | web search) are the
  reference.
- **Connected button group**: toggles inside one pill, the selected one filled with
  `secondaryContainer`. A whole group can be the leading part of a split button, e.g. the wallpaper
  page's `[sun | link | moon]` split from `[pencil]`. Check which pieces form one block before
  splitting things apart.

## Elevation

Flat by default: surfaces are told apart by colour (`surfaceBright` on `pageBackground`), not
shadow. Add a shadow only when something floats over content that might match it:

| What | Shadow |
|---|---|
| Long-press menu | 3dp |
| Controls over the wallpaper preview (`FloatShadow`) | 3dp |
| A row being dragged | 6dp, animated in with the lift |

The drawer sheet has no shadow. It's told apart from home by its solid colour and the flat black scrim
it pulls over home (up to 28%).

The shadow's shape must be the exact outline that's drawn. For a shape with extra parts (the menu
card and its arrow), join them into one path (`Path.combine(PathOperation.Union, …)`) so the shadow
covers both with no seam.

## Layout

- **Edge to edge.** Every screen draws behind the system bars and pads with window insets. Lists run
  to the screen edge and scroll behind floating bars, with bottom padding so the last item can
  scroll clear of them (see `Drawer` and `searchBarSpace()`).
- **Bottom-anchored search.** The launcher's one search bar floats at the bottom over both home and
  the drawer (`MainActivity.kt`). Its gap above the gesture bar shrinks when the keyboard is up
  (`searchBarBottom()`).
- **Results grow upwards.** Order from the top: web search, contacts, then apps nearest the thumb.
  The best match is highlighted with `secondaryContainer`, because Enter opens it.
- **Muscle memory.** Things stay in the same place: Call is always the last piece of a contact row,
  even when WhatsApp is missing.
- **Floating controls** (wallpaper page): top row under the status bar, bottom row lifted by
  `ThumbLift` above the navigation bar.
- **Status and navigation bar icons** follow what's behind them. Set them light over a dark preview,
  as `SystemBarIcons()` does.

## Components

Reuse these before building anything new.

**Shared (`ui/`)**
- `SearchField` (`Common.kt`): pill search bar, `Control` high, search icon when empty, clear button
  when not.
- `LauncherMenu` + `MenuEntry` (`Common.kt`): long-press menu. Each entry is its own `splitShape`
  card with a 2dp gap. With `arrow = true` it points at the item it belongs to. It fades and scales
  in from the arrow.
- `AppIcon` (`Common.kt`): an app icon. On home and in the drawer (`badge = true`) apps with
  notifications get a `primary` dot at the top-end corner, with a thin notch cut into the icon around
  it instead of an outline, so it reads on any background. The dot stays inside the icon's bounds.
  Put clips and clicks in `iconModifier` so they never clip the dot. No counts, no red.
  Home's menu (long-press empty space) is, from the top: Stay awake ("Turn off Stay awake" while
  it's on), Launcher settings, System settings.
- `StayAwakeChip` (`StayAwakeChip.kt`): a tonal chip under the date, only while Stay awake is on, so
  home looks unchanged when it's off. `secondaryContainer` with `onSecondaryContainer` content: coffee
  icon (18dp), "Stay awake on" in `labelLarge`, a small close icon (16dp). `Sizes.Chip` (32dp) to look
  at, 48dp to hit. Its start lines up with the date's text. It fades and expands in, and tapping it
  turns Stay awake off. Locking the phone turns it off too, so there's no notification for it.
- `rememberLaunchSource()` (`Common.kt`): put its modifier on the icon itself, so the app opens with
  a clip reveal from that icon.
- Split rows (`Segment`, `IconSegment` in `Drawer.kt`): result rows. Where a piece opens another app
  (WhatsApp, Phone, browser, Play Store), show that app's own icon, with an `Ic` fallback if it
  isn't installed.

**Settings (`SettingsActivity.kt`)**
- `PageScaffold`: page background, large collapsing top app bar, back button, optional bottom bar.
- `Group(header)`: rows stacked with `SegmentedGap` and a `labelLarge` header.
- `NavRow` (icon, title, chevron), `SwitchRow` (title, `MonoSwitch`), `AppRow`: built on
  `SegmentedListItem` with `itemColors` (`surfaceBright`).
- `Lead`: 40dp circle in `secondaryContainer` behind a row's icon.
- `MonoSwitch`: a Material switch with a check icon in the thumb when on. Rows use plain `onClick`,
  because the "checked" list item tints the whole row.
- `AppListPage`: searchable app list with the search field at the bottom.
- Wallpaper page controls: `RoundButton`, `PillGroup`, `PillToggle`, `SourceButton`. The single
  `primary` round button at the end of the bottom row is whatever needs doing: "Set wallpaper" (check)
  while Pebbles isn't the wallpaper, or "Use on lock screen too" (lock) while the lock screen has its
  own wallpaper. It hides once there's nothing to do.
- A new settings page is a `Page` enum entry with its parent (back goes to the parent) and a case in
  `SettingsRoot`.

**Feedback.** In settings, messages go through `LocalNotify` (a snackbar), not a toast. Toasts are
only for failures in `Actions.kt`, which runs outside Compose.

## Icons

- All icons live in the `Ic` object (`ui/Icons.kt`) as Material Symbols path data, filled style,
  24dp. To add one, paste the symbol's path there. Don't add an icon library.
- Icon-only buttons need a content description. Decorative icons next to text get `null`.

## Motion

- The theme uses `MotionScheme.expressive()`. Take springs from `MaterialTheme.motionScheme` (the
  drawer uses `defaultSpatialSpec`) rather than writing timings by hand.
- **Home ↔ drawer is a sheet, not a crossfade.** The drawer is a solid `pageBackground` sheet that
  rises from under the search bar and moves 1:1 with the finger: a drag across the whole screen opens
  it. On release, a fling or passing halfway decides where it springs. Its top corners go from 28dp
  when closed to square as it reaches the top. Home doesn't fade: it lifts 40dp and shrinks 4% as the
  sheet covers it, under a flat black scrim that rises to 28%. Don't fade home or the drawer's
  contents: fading a layer of content shows the wallpaper through every icon and looks muddy. A flat
  single-colour scrim is the only fade allowed (`LauncherRoot` in `MainActivity.kt`).
- The drawer's progress is read only inside `graphicsLayer` and draw lambdas, so following the finger
  never recomposes. Do the same for anything animated every frame. Closed, the sheet sits fully off
  screen so it can't catch touches meant for home.
- Swiping down on home pulls down the notification shade as soon as the swipe is known to be
  downward (past the touch slop). Android lets only the system drag the shade with the finger, so
  starting it early is as close as a launcher can get. A swipe that has moved the drawer at all never
  opens the shade.
- Tapping the home clock opens the clock app; tapping the date opens the calendar on today. Both grow
  out of the text, like apps from their icons.
- Springs can overshoot. Clamp where an overshoot would show something it shouldn't (the sheet stops
  at the top so wallpaper never peeks under the search bar).
- Apps open with a clip reveal from their icon.
- Menus fade and scale in over 160ms from where the arrow points.
- Settings pages crossfade.
- Rows that move between sections (Hidden apps) glide there with `animateItem` while the rows on
  screen stay put, so it's clear where the row went.
- Reordering a list: other rows glide to their places with `animateItem`. The dragged row follows
  the finger exactly, grows 3% and gains a shadow.

## Orientation

The launcher and its settings stay upright by default ("Portrait only", on unless turned off), on
every device. Turned off, they follow auto-rotate like any app. Layouts must still work sideways.

## Writing

- Sentence case everywhere: "Hidden apps", "Set as default launcher".
- Short, plain labels. Buttons and menu items are verbs: "Add app", "Hide", "Turn off".
- Messages say what happened in a few words: "Settings saved", "Couldn't use that image",
  "That isn't a launcher backup". No "successfully", no error codes.
- Name things the way the system does (Wallpaper & style, Quick Settings, Notification badges).

## Wallpaper

Pebbles (`PebblesWallpaper.kt`, `object Pebbles`) is drawn in a 360×800 space scaled to cover the
screen. The layout is a cascade: three pebbles fall from the top right to the bottom left, growing
as they go, between a soft circle behind the clock and another bleeding off behind the dock, with two
thin rings as accents. No dots. The shapes take the look of the Phone and Messages icons: a vertical
fade from lavender (`top`) to bright blue (`bottom`), with a deeper blue copy (`back`) offset up and
left behind each pebble. The fade runs straight down the screen however a shape is rotated. Light
and dark palettes are kept in step, and `colors()` hands the blues to Material You. Only the soft
circles may sit behind the clock or the dock. Keep pebbles and rings clear of the clock text (check
with the real font on the phone), the dock and the search bar.

## Checking UI work

- Install the release build (`./gradlew -q :app:installRelease`) and look at the change on the
  phone. Debug builds are much slower and misjudge smoothness.
- Check light and dark mode, and an empty and a full state where there is one.
- Check nothing tappable is under 48dp and nothing has a sharp corner.
