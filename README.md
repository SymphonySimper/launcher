# Launcher

A small, fast Android home screen built with Jetpack Compose and Material 3 Expressive. It has
search-first app and contact lookup, a dock, the Pebbles live wallpaper and Stay awake, which keeps
the screen on from home's long-press menu until the phone is locked.

## Releasing

Every push to `main` is built by the Build workflow. To publish a release, fast-forward the `release`
branch to main:

```sh
git push origin main:release
```

The Release workflow then builds a signed APK and publishes it as a GitHub release. The version is
`1.0.<commit count>`, so every release installs over the one before it. Signing uses the secrets
listed at the top of [release.yml](.github/workflows/release.yml).

## Thanks

This launcher borrows ideas from these open-source apps:

- [Lawnchair](https://github.com/LawnchairLauncher/lawnchair): the home screen and long-press menus
- [KISS](https://github.com/Neamar/KISS): search as the main way to open apps and contacts
- [Coffee](https://github.com/mueller-ma/Coffee): keeping the screen on (Stay awake)
- [Doodle](https://github.com/patzly/doodle-android): the light and dark live wallpaper
