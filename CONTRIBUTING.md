# Contributing

Read [AGENTS.md](AGENTS.md) for the performance rules and commit style, and [DESIGN.md](DESIGN.md)
before changing anything visible.

## Building

The [justfile](justfile) has the common tasks; run `just` to list them. `just install` puts the
optimised release build on a connected phone, and `just backup` / `just restore` save and bring back
the launcher's settings over adb. It uses Android Studio's bundled JDK when `JAVA_HOME` isn't set.

## Releasing

Every push to `main` is built by the Build workflow. To publish a release, fast-forward the `release`
branch to main:

```sh
just release   # or: git push origin main:release
```

The Release workflow then builds a signed APK and publishes it as a GitHub release. The version is
`<commit date>.<commit count>` (like `2026.11.27.412`), and the count makes every release install
over the one before it. Signing uses the secrets listed at the top of [release.yml](.github/workflows/release.yml).
