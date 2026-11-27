# AGENTS.md

## Design

Before changing anything visible, read [DESIGN.md](DESIGN.md): colours, sizes, shapes, components and
motion the launcher already uses. Follow it, and add to it when you settle a new pattern.

## Performance

Speed, memory and battery come first, with no trade-offs on any of them. Every change keeps the
launcher fast, lean and idle when nothing is happening:

- **Nothing runs off screen.** Listeners, receivers and timers are tied to the lifecycle
  (`LifecycleStartEffect`), not just to composition, so they stop while another app is open. No
  polling; react to system events.
- **Animate in the draw phase.** Values that change every frame are read only inside `graphicsLayer`
  or draw lambdas, never during composition.
- **Bitmaps are GPU-only** (`Bitmap.Config.HARDWARE`) unless their pixels must be read, and decoded no
  larger than they're shown.
- **Prove it on the release build** (`./gradlew -q :app:installRelease`): compare `dumpsys meminfo`
  and `dumpsys gfxinfo` before and after anything that might cost memory or frames.
- **Keep the baseline profile current.** `baselineprofile/` records the code home, the drawer and
  search run, so it's compiled ahead of time; the result is committed in `app/src/release/generated`.
  After adding a screen or reworking a main path, extend `Journeys.kt` and regenerate with
  `./gradlew :app:generateBaselineProfile -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`
  (the flag keeps Gradle from uninstalling the launcher and wiping its settings). It restarts the app,
  so set the wallpaper and default home again afterwards. `LauncherBenchmarks` measures the gain.
- **Back up the phone's settings before testing** anything that might reinstall or reset the
  launcher (baseline profile, benchmarks, connected tests), and restore them afterwards:
  `adb shell dumpsys activity com.symphonysimper.launcher/.MainActivity export-settings` prints the
  backup JSON (keep the part from `{` to `}`), and `... import-settings $(base64 -w0 backup.json)`
  restores it. The launcher must be on screen for both. An uninstall also drops notification access
  for badges; give it back with
  `adb shell cmd notification allow_listener com.symphonysimper.launcher/com.symphonysimper.launcher.BadgeService`.
- Don't use `setFixedSize` on the wallpaper surface: wallpapers must size from layout, and the crash
  resets the user's wallpaper and default home app.

## Code

- **No comments that explain our own code.** Make names and types say it instead: an enum, not a
  nullable `Boolean`; `suspend` on IO, not "call off the main thread". Only comment on behaviour of
  Android or a library that the code can't show.
- **No dead code.** Remove unused parameters, defaults every caller overrides, and branches that can't
  run, rather than keeping them for later.

## Commit messages

Use the format `<scope>: <message>`.

- `<scope>` is the area of the codebase the change touches, in lowercase (e.g. `app`, `ui`, `theme`, `build`, `deps`, `docs`, `repo`).
- Use `repo` (not `project`) for repository-wide changes such as scaffolding, `.gitignore`, IDE or CI config; "project" is ambiguous with the Gradle project.
- `<message>` is a short, imperative summary in lowercase (e.g. "add app drawer", not "Added app drawer.").
- Keep the subject line under ~72 characters with no trailing period. Add a body after a blank line if the change needs explanation.

Examples:

```
ui: add app drawer grid
build: bump compose bom to latest
docs: add commit convention to agents.md
```
